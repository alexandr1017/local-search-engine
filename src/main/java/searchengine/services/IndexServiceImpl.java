package searchengine.services;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.exceptions.IncorrectURIException;
import searchengine.exceptions.IndexingAlreadyStartedException;
import searchengine.model.IndexModel;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.util.LemmaFinder;
import searchengine.util.RecursiveSiteCrawler;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;

import java.util.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Service
public class IndexServiceImpl implements IndexingService {

    private static final int TERMINATION_AWAIT_TIME_HOURS = 24;
    @Autowired
    private SitesList sitesList;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;

    public static volatile Boolean isRunning = false;
    private static volatile Boolean isStopped = false;
    private ForkJoinPool pool;


    @Override
    public IndexingResponse startIndexing() {
        if (isRunning) {
            throw new IndexingAlreadyStartedException("Индексация уже запущена");
        } else {
            isRunning = true;
            new Thread(this::sitesParsing).start();
            return new IndexingResponse(true);
        }
    }


    public IndexingResponse sitesParsing() {

        List<Site> sites = sitesList.getSites();

        for (Site site : sites) {
            siteRepository.deleteByUrl(site.getUrl());

            SiteModel siteModel = new SiteModel();
            siteModel.setName(site.getName());
            siteModel.setUrl(site.getUrl());

            siteRepository.save(siteModel);
        }

        List<SiteModel> allSiteModels = siteRepository.findAll();


        for (SiteModel siteModel : allSiteModels) {

            Set<String> visitedLinks = ConcurrentHashMap.newKeySet();
            RecursiveSiteCrawler recursiveSiteCrawlerTask = new RecursiveSiteCrawler(siteModel.getUrl(), visitedLinks, siteModel, siteRepository, pageRepository, lemmaRepository, indexRepository);

            pool = new ForkJoinPool();
            pool.execute(recursiveSiteCrawlerTask);

            pool.shutdown();
            awaitPoolTermination();

            if (isStopped) {
                isStopped = false;
                throw new IndexingAlreadyStartedException("Индексация остановлена пользователем");
            }

            SiteModel siteModelFromDb = siteRepository.findByUrl(siteModel.getUrl()).get();
            String lastError = siteModelFromDb.getLastError();

            if (lastError == null) {
                updateSiteStatus(siteModel, "INDEXED", "");
            } else if (lastError.startsWith("Ошибка")) {
                updateSiteStatus(siteModel, "FAILED", "Ошибка. Нет доступа к сайту");
            }

        }

        isRunning = false;
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse stopIndexing() throws InterruptedException {
        if (!isRunning) {
            throw new IndexingAlreadyStartedException("Индексация не запущена");
        } else {
            isRunning = false;
            isStopped = true;
        }
        pool.shutdown();
        awaitPoolTermination();

        List<SiteModel> siteList = siteRepository.findAll();
        for (SiteModel siteModel : siteList) {


            if (siteModel.getStatus().equals("INDEXING")) {

                siteModel.setStatusTime(LocalDateTime.now());
                siteModel.setStatus("FAILED");
                siteModel.setLastError("Индексация остановлена пользователем");
                siteRepository.save(siteModel);
            }


        }
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse addPageToIndex(String uri) throws IOException {
        URL url = new URL(uri);
        String path = url.getPath();
        String siteUrl = uri.replaceAll("^(https?://[^/]+)(/.*)?$", "$1");

        if (searchSiteInConfig(siteUrl) == null) {
            throw new IncorrectURIException("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }

        if (!uri.matches(RecursiveSiteCrawler.REGEX_URL)) {
            throw new IncorrectURIException("Ошибочный адрес страницы");
        }

        Optional<PageModel> pageModelOptional = pageRepository.findByPath(path);
        Optional<SiteModel> siteModelOptional = siteRepository.findByUrl(siteUrl);

        boolean isPageExist = pageModelOptional.isPresent();
        boolean isSiteExist = siteModelOptional.isPresent();

        try {
            Document docItem = Jsoup.connect(uri).get();
            String html = docItem.html();

            Map<String, Integer> lemmasCountMap = LemmaFinder.getInstance().wordAndCountsCollector(html);

            if (!isPageExist) {
                if (!isSiteExist) {
                    SiteModel siteModel = createNewSite(siteUrl);
                    PageModel pageModel = createNewPage(path, html, siteModel);

                    lemmaAndIndexBuilder(lemmasCountMap, siteModel, pageModel);

                    updateSiteStatus(siteModel, "INDEXED", "");
                } else {
                    SiteModel siteModel = siteModelOptional.get();
                    PageModel pageModel = createNewPage(path, html, siteModel);

                    lemmaAndIndexBuilder(lemmasCountMap, siteModel, pageModel);

                    updateSiteStatus(siteModel, "INDEXED", "");

                }
            } else {
                SiteModel siteModel = siteModelOptional.get();
                PageModel pageModel = pageModelOptional.get();

                updatePageContent(pageModel, html);

                lemmaAndIndexBuilder(lemmasCountMap, siteModel, pageModel);

                updateSiteStatus(siteModel, "INDEXED", "");
            }

            return new IndexingResponse(true);

        } catch (HttpStatusException ex) {
            if (!isSiteExist) {
                createNewSite(siteUrl);
            }
            handleHttpStatusException(ex, path, siteUrl);
            throw new IncorrectURIException("Страница не доступна. Код: " + ex.getStatusCode());
        }
    }

    private void lemmaAndIndexBuilder(Map<String, Integer> lemmasCountMap, SiteModel siteModel, PageModel pageModel) {
        lemmasCountMap.forEach((lemma, rate) -> {
            LemmaModel lemmaModel = new LemmaModel();
            lemmaModel.setLemma(lemma);
            lemmaModel.setSiteId(siteModel);
            lemmaModel = addLemmaToDB(lemmaModel);

            IndexModel indexModel = new IndexModel();
            indexModel.setLemmaId(lemmaModel);
            indexModel.setPageId(pageModel);
            indexModel.setRank(rate);
            lemmaRepository.save(lemmaModel);
            indexRepository.save(indexModel);
        });
    }

    private LemmaModel addLemmaToDB(LemmaModel lemmaModel) {
        List<LemmaModel> lemmaList = lemmaRepository.findByLemma(lemmaModel.getLemma());

        if (lemmaList.size() == 0) {
            lemmaModel.setFrequency(1);
            return lemmaRepository.save(lemmaModel);
        } else {
            LemmaModel lemmaFromDb = lemmaList.get(0);
            lemmaFromDb.setFrequency(lemmaFromDb.getFrequency() + 1);
            return lemmaRepository.save(lemmaFromDb);
        }
    }

    private void handleHttpStatusException(HttpStatusException ex, String path, String siteUrl) {
        SiteModel siteModel = siteRepository.findByUrl(siteUrl).get();
        PageModel pageModel = new PageModel();

        pageModel.setCode(ex.getStatusCode());
        pageModel.setPath(path);
        pageModel.setContent("error");
        pageModel.setSiteId(siteModel);
        pageRepository.save(pageModel);

        siteModel.setStatus("FAILED");
        siteModel.setStatusTime(LocalDateTime.now());
        siteModel.setLastError("Индексация страницы не выполнена. Страница не доступна. Код: " + ex.getStatusCode());
        siteRepository.save(siteModel);
    }

    private void updateSiteStatus(SiteModel siteModel, String status, String lastError) {
        siteModel.setLastError(lastError);
        siteModel.setStatus(status);
        siteModel.setStatusTime(LocalDateTime.now());
        siteRepository.save(siteModel);
    }

    private PageModel createNewPage(String path, String html, SiteModel siteModel) {
        PageModel pageModel = new PageModel();
        pageModel.setContent(html);
        pageModel.setCode(200);
        pageModel.setSiteId(siteModel);
        pageModel.setPath(path);
        return pageRepository.save(pageModel);
    }

    private SiteModel createNewSite(String siteUrl) {
        SiteModel siteModel = new SiteModel();
        Site site = searchSiteInConfig(siteUrl);
        siteModel.setName(site.getName());
        siteModel.setUrl(siteUrl);
        return siteRepository.save(siteModel);
    }

    private void updatePageContent(PageModel pageModel, String content) {
        pageModel.setContent(content);
        pageRepository.save(pageModel);
    }

    private void awaitPoolTermination() {
        try {
            if (!pool.awaitTermination(TERMINATION_AWAIT_TIME_HOURS, TimeUnit.HOURS)) {
                awaitPoolTermination();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Site searchSiteInConfig(String siteUrl) {
        for (Site site : sitesList.getSites()) {
            if (site.getUrl().equals(siteUrl)) {
                return site;
            }
        }
        return null;
    }
}

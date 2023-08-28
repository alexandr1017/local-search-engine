package searchengine.services;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.exceptions.IncorrectURIException;
import searchengine.exceptions.IndexingAlreadyStartedException;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.util.LemmaFinder;
import searchengine.util.RecursiveSiteCrawler;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.time.LocalDateTime;

import java.util.*;

import java.util.concurrent.ConcurrentHashMap;
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


    private void sitesParsing() {

        List<SiteConfig> siteConfigs = sitesList.getSites();

        for (SiteConfig siteConfig : siteConfigs) {
            siteRepository.deleteByUrl(siteConfig.getUrl());

            Site site = new Site();
            site.setName(siteConfig.getName());
            site.setUrl(siteConfig.getUrl());

            siteRepository.save(site);
        }

        List<Site> allSites = siteRepository.findAll();


        for (Site site : allSites) {

            Set<String> visitedLinks = ConcurrentHashMap.newKeySet();
            RecursiveSiteCrawler recursiveSiteCrawlerTask = new RecursiveSiteCrawler(site.getUrl(), visitedLinks, site, siteRepository, pageRepository, lemmaRepository, indexRepository);

            pool = new ForkJoinPool();
            pool.execute(recursiveSiteCrawlerTask);

            pool.shutdown();
            awaitPoolTermination();

            if (isStopped) {
                isStopped = false;
                throw new IndexingAlreadyStartedException("Индексация остановлена пользователем");
            }

            Site siteFromDb = siteRepository.findByUrl(site.getUrl()).get();
            String lastError = siteFromDb.getLastError();

            if (lastError == null) {
                updateSiteStatus(site, "INDEXED", "");
            } else if (lastError.startsWith("Ошибка")) {
                updateSiteStatus(site, "FAILED", "Ошибка индексации: главная страница сайта не доступна");
            }
        }
        isRunning = false;
    }

    @Override
    public IndexingResponse stopIndexing() {
        if (!isRunning) {
            throw new IndexingAlreadyStartedException("Индексация не запущена");
        } else {
            isRunning = false;
            isStopped = true;
        }
        pool.shutdown();
        awaitPoolTermination();

        List<Site> siteList = siteRepository.findAll();
        for (Site site : siteList) {

            if (site.getStatus().equals("INDEXING")) {

                site.setStatusTime(LocalDateTime.now());
                site.setStatus("FAILED");
                site.setLastError("Индексация остановлена пользователем");
                siteRepository.save(site);
            }
        }
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse addPageToIndex(String uri) throws IOException {
        String[] split = URLDecoder.decode(uri).split("=");
        String urlDecode = split[1];
        URL url = new URL(urlDecode);
        String path = url.getPath();
        String siteUrl = urlDecode.replaceAll("^(https?://[^/]+)(/.*)?$", "$1");

        if (searchSiteInConfig(siteUrl) == null) {
            throw new IncorrectURIException("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }

        if (!urlDecode.matches(RecursiveSiteCrawler.REGEX_URL)) {
            throw new IncorrectURIException("Ошибочный адрес страницы");
        }

        Optional<Page> pageModelOptional = pageRepository.findByPath(path);
        Optional<Site> siteModelOptional = siteRepository.findByUrl(siteUrl);

        boolean isPageExist = pageModelOptional.isPresent();
        boolean isSiteExist = siteModelOptional.isPresent();

        try {
            Document docItem = Jsoup.connect(urlDecode).get();
            String html = docItem.html();

            Map<String, Integer> lemmasCountMap = LemmaFinder.getInstance().wordAndCountsCollector(html);

            if (!isPageExist) {
                if (!isSiteExist) {
                    Site site = createNewSite(siteUrl);
                    Page page = createNewPage(path, html, site);

                    lemmaAndIndexBuilder(lemmasCountMap, site, page);

                    updateSiteStatus(site, "INDEXED", "");
                } else {
                    Site site = siteModelOptional.get();
                    Page page = createNewPage(path, html, site);

                    lemmaAndIndexBuilder(lemmasCountMap, site, page);

                    updateSiteStatus(site, "INDEXED", "");

                }
            } else {
                Site site = siteModelOptional.get();
                Page page = pageModelOptional.get();

                updatePageContent(page, html);

                lemmaAndIndexBuilder(lemmasCountMap, site, page);

                updateSiteStatus(site, "INDEXED", "");
            }

            return new IndexingResponse(true);

        } catch (HttpStatusException ex) {
            if (!isSiteExist) {
                createNewSite(siteUrl);
            }
            handlePageHttpStatusException(ex, path, siteUrl);
            throw new IncorrectURIException("Страница не доступна. Код: " + ex.getStatusCode());
        }
    }

    private void lemmaAndIndexBuilder(Map<String, Integer> lemmasCountMap, Site site, Page page) {
        lemmasCountMap.forEach((lemma, rate) -> {
            Lemma lemmaModel = new Lemma();
            lemmaModel.setLemma(lemma);
            lemmaModel.setSiteId(site);
            lemmaModel = addLemmaToDB(lemmaModel);

            Index index = new Index();
            index.setLemmaId(lemmaModel);
            index.setPageId(page);
            index.setRank(rate);
            lemmaRepository.save(lemmaModel);
            indexRepository.save(index);
        });
    }

    private Lemma addLemmaToDB(Lemma lemma) {
        List<Lemma> lemmaList = lemmaRepository.findByLemma(lemma.getLemma());

        if (lemmaList.size() == 0) {
            lemma.setFrequency(1);
            return lemmaRepository.save(lemma);
        } else {
            Lemma lemmaFromDb = lemmaList.get(0);
            lemmaFromDb.setFrequency(lemmaFromDb.getFrequency() + 1);
            return lemmaRepository.save(lemmaFromDb);
        }
    }

    private void handlePageHttpStatusException(HttpStatusException ex, String path, String siteUrl) {
        Site site = siteRepository.findByUrl(siteUrl).get();
        Page page = new Page();

        page.setCode(ex.getStatusCode());
        page.setPath(path);
        page.setContent("error");
        page.setSiteId(site);
        pageRepository.save(page);

        site.setStatus("FAILED");
        site.setStatusTime(LocalDateTime.now());
        site.setLastError("Ошибка индексации: страница не доступна");
        siteRepository.save(site);
    }

    private void updateSiteStatus(Site site, String status, String lastError) {
        site.setLastError(lastError);
        site.setStatus(status);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
    }

    private Page createNewPage(String path, String html, Site site) {
        Page page = new Page();
        page.setContent(html);
        page.setCode(200);
        page.setSiteId(site);
        page.setPath(path);
        return pageRepository.save(page);
    }

    private Site createNewSite(String siteUrl) {
        Site site = new Site();
        SiteConfig siteConfig = searchSiteInConfig(siteUrl);
        site.setName(siteConfig.getName());
        site.setUrl(siteUrl);
        return siteRepository.save(site);
    }

    private void updatePageContent(Page page, String content) {
        page.setContent(content);
        pageRepository.save(page);
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

    private SiteConfig searchSiteInConfig(String siteUrl) {
        for (SiteConfig siteConfig : sitesList.getSites()) {
            if (siteConfig.getUrl().equals(siteUrl)) {
                return siteConfig;
            }
        }
        return null;
    }
}

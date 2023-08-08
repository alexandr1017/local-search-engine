package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.IndexingResponseFail;
import searchengine.exceptions.IncorrectURIException;
import searchengine.exceptions.IndexingAlreadyStartedException;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.util.RecursiveSiteCrawler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;

import java.util.List;

import java.util.Optional;
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

            RecursiveSiteCrawler recursiveSiteCrawlerTask = new RecursiveSiteCrawler(siteModel.getUrl(), siteModel, siteRepository, pageRepository);
            pool = new ForkJoinPool();
            pool.execute(recursiveSiteCrawlerTask);

            pool.shutdown();
            awaitPoolTermination();

            if (isStopped) {
                isStopped = false;
                throw new IndexingAlreadyStartedException("Индексация остановлена пользователем");
            }

            siteModel.setStatusTime(LocalDateTime.now());
            siteModel.setStatus("INDEXED");
            siteRepository.save(siteModel);

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

            if (!isPageExist) {
                if (!isSiteExist) {
                    SiteModel siteModel = createNewSite(siteUrl);
                    createNewPage(path, html, siteModel);
                } else {
                    SiteModel siteModel = siteModelOptional.get();
                    createNewPage(path, html, siteModel);
                    updateSiteStatus(siteModel, "INDEXED");
                }
            } else {
                SiteModel siteModel = siteModelOptional.get();
                PageModel pageModel = pageModelOptional.get();
                updatePageContent(pageModel, html);
                updateSiteStatus(siteModel, "INDEXED");
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

    private void updateSiteStatus(SiteModel siteModel, String status) {
        siteModel.setLastError("");
        siteModel.setStatus(status);
        siteModel.setStatusTime(LocalDateTime.now());
        siteRepository.save(siteModel);
    }

    private void createNewPage(String path, String html, SiteModel siteModel) {
        PageModel pageModel = new PageModel();
        pageModel.setContent(html);
        pageModel.setCode(200);
        pageModel.setSiteId(siteModel);
        pageModel.setPath(path);
        pageRepository.save(pageModel);
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

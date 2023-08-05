package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.IndexingResponseFail;
import searchengine.exceptions.IndexingAlreadyStartedException;
import searchengine.model.SiteModel;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.util.RecursiveSiteCrawler;

import java.time.LocalDateTime;

import java.util.List;

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
        for (SiteModel site : siteList) {
            if (site.getStatus().equals("INDEXING")) {

                site.setStatusTime(LocalDateTime.now());
                site.setStatus("FAILED");
                site.setLastError("Индексация остановлена пользователем");

                siteRepository.save(site);
            }
        }
        return new IndexingResponse(true);
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

}
//TODO: Применить синглтон??
//    private IndexServiceImpl() {
//        // Конструктор
//    }
//
//    public static IndexServiceImpl getInstance() {
//        if (instance == null) {
//            instance = new IndexServiceImpl();
//        }
//        return instance;
//    }
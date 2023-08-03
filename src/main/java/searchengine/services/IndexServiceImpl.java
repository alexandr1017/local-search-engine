package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.IndexingResponseFail;
import searchengine.model.SiteModel;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.util.GogoLink;

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
            return new IndexingResponseFail("Индексация уже запущена");
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

            SiteModel savedSiteModel = siteRepository.save(siteModel);


            GogoLink gogoLinkTask = new GogoLink(siteModel.getUrl(), savedSiteModel, siteRepository, pageRepository);
            pool = new ForkJoinPool();
            pool.execute(gogoLinkTask);

            pool.shutdown();
            awaitPoolTermination();

            if (isStopped) {
                isStopped = false;
                return new IndexingResponseFail("Индексация остановлена пользователем");
            }

            siteModel.setStatusTime(LocalDateTime.now());
            siteModel.setStatus("INDEXED");
            siteRepository.save(siteModel);

        }


        isRunning = false;
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse stopIndexing() {
        if (!isRunning) {
            return new IndexingResponseFail("Индексация не запущена");
        } else {
            isRunning = false;
            isStopped = true;
        }
        pool.shutdown();

        List<SiteModel> siteList = siteRepository.findAll();

        for (SiteModel site : siteList) {
            if (site.getStatus().equals("INDEXING")){

                siteRepository.delete(site);

                SiteModel siteModel = new SiteModel();
                siteModel.setName(site.getName());
                siteModel.setUrl(site.getUrl());
                siteModel.setStatusTime(LocalDateTime.now());
                siteModel.setStatus("FAILED");
                siteModel.setLastError("Индексация остановлена пользователем");

                siteRepository.save(siteModel);
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
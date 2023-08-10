package searchengine.util;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.dao.DataIntegrityViolationException;
import searchengine.model.IndexModel;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexServiceImpl;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RecursiveAction;

public class RecursiveSiteCrawler extends RecursiveAction {
    public static final String REGEX_URL = "^(https?|ftp|file)://[-a-zA-Z0-9+&@/%=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
    public static final String USER_AGENT = "GoodSearchBot";
    public static final String REFERRER = "http://www.google.com";

    private String url;
    private SiteModel siteId;
    private PageModel pageModel;

    CopyOnWriteArrayList<String> visitedLinks;
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;
    private IndexRepository indexRepository;


    public RecursiveSiteCrawler(String url, CopyOnWriteArrayList<String> visitedLinks, SiteModel siteId, SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.url = url;
        this.siteId = siteId;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.visitedLinks = visitedLinks;
    }

    @Override
    protected void compute() {

        if (!IndexServiceImpl.isRunning) {
            return;
        }
        try {
            Thread.sleep(250);

            System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
            Connection.Response res = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .referrer(REFERRER)
                    .ignoreContentType(true).execute();

            int statusCode = res.statusCode();
            Document doc = res.parse();
            Elements links = doc.select("a");

            List<RecursiveSiteCrawler> subTasks = new ArrayList<>();

            for (Element item : links) {
                String href = item.attr("abs:href");

                if (visitedLinks.contains(href)) continue;

                if (href.startsWith(url)
                        && href.matches(REGEX_URL)
                        && !href.equals(url)
                        && !href.contains("#")
                        && !href.endsWith("pdf")
                        && !href.endsWith("jpg")
                        && !href.endsWith("jpeg")
                        && !href.endsWith("png")) {

                    URL hrefUrl = new URL(href);
                    String path = hrefUrl.getPath();

                    visitedLinks.add(href);

                    pageModel = new PageModel();
                    pageModel.setPath(path);

                    Document docItem = Jsoup.connect(href).get();
                    String html = docItem.html();

                    Map<String, Integer> lemmasCountMap = LemmaFinder.getInstance().wordAndCountsCollector(html);


                    pageModel.setContent(html);
                    pageModel.setCode(statusCode);
                    pageModel.setSiteId(siteId);
                    pageRepository.save(pageModel);

                    lemmaAndIndexBuilder(lemmasCountMap, siteId, pageModel);

                    siteId.setStatusTime(LocalDateTime.now());
                    siteRepository.save(siteId);


                    if (!IndexServiceImpl.isRunning) {
                        return;
                    }

                    RecursiveSiteCrawler subTask = new RecursiveSiteCrawler(href, visitedLinks, siteId, siteRepository, pageRepository, lemmaRepository, indexRepository);
                    subTask.fork();
                    subTasks.add(subTask);
                }
            }

            subTasks.sort(Comparator.comparing(o -> o.url));
            invokeAll(subTasks);

        } catch (HttpStatusException e) {
            e.printStackTrace();
//            handleHttpStatusException(e); //TODO: Написать обработчик ошибок
        } catch (Exception e) {
            e.printStackTrace();
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

//    private void handleHttpStatusException(HttpStatusException ex) {
//        SiteModel siteModel = siteRepository.findByUrl(siteId.getUrl()).get();
//        System.out.println(siteModel);
//        PageModel pageModel = new PageModel();
//
//        String path = ex.getUrl().replaceAll("https?://[^/]+(/[^?#]*)", "$1");
//
//        pageModel.setCode(ex.getStatusCode());
//        pageModel.setPath(path);
//        pageModel.setContent("error");
//        pageModel.setSiteId(siteModel);
//        pageRepository.save(pageModel);
//
//        if (path.equals("/")) {
//            siteModel.setLastError("Ошибка доступа");
//        }
//        String lastError = siteModel.getLastError();
//        System.out.println(lastError);
//        siteModel.setStatusTime(LocalDateTime.now());
//        siteRepository.save(siteModel);
//    }
}


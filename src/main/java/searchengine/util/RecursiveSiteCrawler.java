package searchengine.util;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexServiceImpl;


import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.RecursiveAction;

public class RecursiveSiteCrawler extends RecursiveAction {
    public static final String REGEX_URL = "^(https?|ftp|file)://[-a-zA-Z0-9+&@/%=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
    public static final String USER_AGENT = "Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6";
    public static final String REFERRER = "http://www.google.com";

    private String url;
    private Site siteId;
    private Set<String> visitedLinks;
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;
    private IndexRepository indexRepository;


    public RecursiveSiteCrawler(String url, Set<String> visitedLinks, Site siteId, SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository) {
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
            Thread.sleep((int) ((Math.random() * 150) + 300));

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
                String link = item.attr("abs:href");

                if (visitedLinks.contains(link)) continue;

                if (link.startsWith(url)
                        && link.matches(REGEX_URL)
                        && !link.equals(url)
                        && !link.contains("#")
                        && !link.endsWith("pdf")
                        && !link.endsWith("jpg")
                        && !link.endsWith("jpeg")
                        && !link.endsWith("png")) {

                    URL hrefUrl = new URL(link);
                    String path = hrefUrl.getPath();

                    visitedLinks.add(link);

                    Document docItem = Jsoup.connect(link).get();
                    String html = docItem.html();

                    Map<String, Integer> lemmasCountMap = LemmaFinder.getInstance().wordAndCountsCollector(html);

                    Page page = createPageModel(statusCode, path, html);

                    lemmaAndIndexBuilder(lemmasCountMap, siteId, page);

                    updateSiteModelDateTime(siteId);


                    if (!IndexServiceImpl.isRunning) {
                        return;
                    }

                    RecursiveSiteCrawler subTask = new RecursiveSiteCrawler(link, visitedLinks, siteId, siteRepository, pageRepository, lemmaRepository, indexRepository);
                    subTask.fork();
                    subTasks.add(subTask);
                }
            }

            subTasks.sort(Comparator.comparing(o -> o.url));
            invokeAll(subTasks);

        } catch (HttpStatusException e) {
            e.printStackTrace();
            handleHttpStatusException(e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Page createPageModel(int statusCode, String path, String html) {
        Page page = new Page();
        page.setPath(path);
        page.setContent(html);
        page.setCode(statusCode);
        page.setSiteId(siteId);
        return pageRepository.save(page);
    }

    private void updateSiteModelDateTime(Site siteId) {
        siteId.setStatusTime(LocalDateTime.now());
        siteRepository.save(siteId);
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

    private void handleHttpStatusException(HttpStatusException ex) {
        Site site = siteRepository.findByUrl(siteId.getUrl()).get();

        String path = ex.getUrl().replaceAll("https?://[^/]+(/[^?#]*)", "$1");

        Page page = new Page();
        page.setCode(ex.getStatusCode());
        page.setPath(path);
        page.setContent("error");
        page.setSiteId(site);
        pageRepository.save(page);

        if (path.startsWith("htt")) {
            site.setLastError("Ошибка");
        }

        updateSiteModelDateTime(site);
    }
}


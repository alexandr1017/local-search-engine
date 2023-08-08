package searchengine.util;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexServiceImpl;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.RecursiveAction;

public class RecursiveSiteCrawler extends RecursiveAction {
    public static final String REGEX_URL = "^(https?|ftp|file)://[-a-zA-Z0-9+&@/%=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
    public static final String USER_AGENT = "GoodSearchBot";
    public static final String REFERRER = "http://www.google.com";

    private String url;
    private SiteModel siteId;
    private PageModel pageModel;
    private SiteRepository siteRepository;
    private PageRepository pageRepository;


    public RecursiveSiteCrawler(String url, SiteModel siteId, SiteRepository siteRepository, PageRepository pageRepository) {
        this.url = url;
        this.siteId = siteId;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
    }

    @Override
    protected void compute() {

        if (!IndexServiceImpl.isRunning) {
            return;
        }
        try {
            Thread.sleep(150);

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

                    boolean isPageExist = pageRepository.findByPath(path).isPresent();

                    if (!isPageExist) {

                        Document docItem = Jsoup.connect(href).get();
                        String html = docItem.html();

                        pageModel = new PageModel();
                        pageModel.setPath(path);
                        pageModel.setContent(html);
                        pageModel.setCode(statusCode);
                        pageModel.setSiteId(siteId);
                        pageRepository.save(pageModel);

                        siteId.setStatusTime(LocalDateTime.now());
                        siteRepository.save(siteId);


                        if (!IndexServiceImpl.isRunning) {
                            return;
                        }

                        RecursiveSiteCrawler subTask = new RecursiveSiteCrawler(href, siteId, siteRepository, pageRepository);
                        subTask.fork();
                        subTasks.add(subTask);
                    }
                }
            }
            subTasks.sort(Comparator.comparing(o -> o.url));
            invokeAll(subTasks);

        } catch (DataIntegrityViolationException e) {
            System.out.println("Ошибка: Невозможно сохранить дубликат path: " + pageModel.getPath());
        } catch (HttpStatusException e) {
            pageModel.setCode(e.getStatusCode());
            pageRepository.save(pageModel);
            System.out.println("Ошибка получения страницы: Код - " + e.getStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


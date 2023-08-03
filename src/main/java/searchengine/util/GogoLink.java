package searchengine.util;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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

public class GogoLink extends RecursiveAction {
    public static final String REGEX_URL = "^(https?|ftp|file)://[-a-zA-Z0-9+&@/%=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";


    private String url;

    private final SiteModel siteId;
    private final PageModel pageModel;


    private SiteRepository siteRepository;


    private PageRepository pageRepository;


    public GogoLink(String url, SiteModel siteId, SiteRepository siteRepository, PageRepository pageRepository) {
        this.url = url;
        this.siteId = siteId;
        this.pageModel = new PageModel();
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
            Connection.Response res = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)" +
                    "AppleWebKit/537.36 (KHTML, like Gecko)" +
                    "Chrome/58.0.3029.110 Safari/537.3").ignoreContentType(true).execute();


            int statusCode = res.statusCode();
            Document doc = res.parse();
            Elements links = doc.select("a");


            List<GogoLink> subTasks = new ArrayList<>();

            for (Element item : links) {

//                if (Thread.currentThread().isInterrupted()) {
//                    return;
//                }

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


                        pageModel.setPath(path);
                        pageModel.setContent(html);
                        pageModel.setCode(statusCode);
                        pageModel.setSiteId(siteId);


                        siteId.setStatusTime(LocalDateTime.now());
                        siteRepository.save(siteId);

                        pageRepository.save(pageModel);

                        if (!IndexServiceImpl.isRunning) {
                            return;
                        }


                        GogoLink subTask = new GogoLink(href, siteId, siteRepository, pageRepository);
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

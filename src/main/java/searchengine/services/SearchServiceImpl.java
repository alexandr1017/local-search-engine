package searchengine.services;

import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchItem;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchResponsePositive;
import searchengine.exceptions.SearchException;
import searchengine.model.IndexModel;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.util.LemmaFinder;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private IndexRepository indexRepository;
    @Autowired
    private PageRepository pageRepository;


    @Override
    public SearchResponse search(String query, String site, int offset, int limit) throws IOException {

        Optional<SiteModel> siteModelOptional = siteRepository.findByUrl(site);

        if (siteModelOptional.isEmpty() && !site.isEmpty()) {
            throw new SearchException("Данный сайт не проиндексирован");
        }

        Set<String> lemmasQuerySet = LemmaFinder.getInstance()
                .wordAndCountsCollector(query)
                .keySet();

        List<LemmaModel> lemmaModelsListOfDB = searchLemmasFromDB(site, siteModelOptional, lemmasQuerySet);

        if (lemmaModelsListOfDB.isEmpty()) {
            throw new SearchException("Поиск не дал результатов");
        }

        List<LemmaModel> sortedLemmasListToFreq = lemmaModelsListOfDB.stream()
                .sorted(Comparator.comparing(LemmaModel::getFrequency))
                .toList();

        List<PageModel> pages = findPages(sortedLemmasListToFreq);

        if (pages.isEmpty()) {
            throw new SearchException("Ничего не найдено по данному запросу");
        }

        Map<PageModel, Double> relevanceMap = buildRelevanceMap(sortedLemmasListToFreq, pages);

        Map<PageModel, Double> sortedRelevanceMap = relevanceMap.entrySet()
                .stream()
                .sorted(Map.Entry.<PageModel, Double>comparingByValue().reversed()).limit(500)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new
                ));


        List<SearchItem> data = buildDataResult(query, sortedRelevanceMap);

        return new SearchResponsePositive(data.size(), data);
    }

    private List<LemmaModel> searchLemmasFromDB(String site, Optional<SiteModel> siteModelOptional, Set<String> lemmasQuerySet) {
        List<LemmaModel> lemmaModelsListOfDB = new ArrayList<>();
        for (String lemma : lemmasQuerySet) {
            LemmaModel lemmaModel;

            if (site == null || site.isEmpty()) {
                lemmaModel = lemmaRepository.customSelectAllSitesFromLemmaDB(lemma);
            } else {
                lemmaModel = lemmaRepository.customSelectFromLemmaDB(siteModelOptional.get().getId(), lemma);
            }

            if (lemmaModel == null) continue;

            lemmaModelsListOfDB.add(lemmaModel);
        }
        return lemmaModelsListOfDB;
    }

    private List<SearchItem> buildDataResult(String query, Map<PageModel, Double> sortedRelevanceMap) {
        List<SearchItem> data = new ArrayList<>();

        sortedRelevanceMap.forEach((page, relevance) -> {

            SearchItem searchItem = new SearchItem();
            searchItem.setRelevance(relevance);
            searchItem.setSite(page.getSiteId().getUrl());
            searchItem.setSiteName(page.getSiteId().getName());
            searchItem.setUri(page.getPath());
            searchItem.setTitle(getTitle(page));
            searchItem.setSnippet(getSnippet(page.getContent(), query));
            data.add(searchItem);
        });
        return data;
    }

    private Map<PageModel, Double> buildRelevanceMap(List<LemmaModel> sortedLemmasListToFreq, List<PageModel> pages) {
        HashMap<PageModel, Double> relevanceMap = new HashMap<>();
        double maxAbsRelevance = 0.0;
        for (PageModel page : pages) {
            double sumRank = 0.0;
            for (LemmaModel lemmaModel : sortedLemmasListToFreq) {
                List<IndexModel> indexModels = indexRepository.findByPageIdAndLemmaId(page.getId(), lemmaModel.getId());
                for (IndexModel index : indexModels) {
                    sumRank += index.getRank();
                }
            }
            maxAbsRelevance = Math.max(sumRank, maxAbsRelevance);
            relevanceMap.put(page, sumRank);
        }
        double finalMaxAbsRelevance = maxAbsRelevance;
        relevanceMap.forEach((page, sumRank) -> {
            relevanceMap.put(page, sumRank / finalMaxAbsRelevance);
        });

        return relevanceMap;
    }

    private List<PageModel> findPages(List<LemmaModel> sortedLemmasListToFreq) {
        List<PageModel> pages = new ArrayList<>();
        for (LemmaModel lemmaModel : sortedLemmasListToFreq) {
            List<Integer> pageIds = indexRepository.findPageIdLemmaId(lemmaModel.getId());
            if (pages.isEmpty()) {
                pages = pageRepository.findPagesByIds(pageIds);
            } else {
                pages = pages.stream()
                        .filter(page -> pageIds.contains(page.getId()))
                        .collect(Collectors.toList());
            }
            if (pages.isEmpty()) {
                return Collections.emptyList();
            }
        }
        return pages;
    }

    private String getSnippet(String htmlContent, String query) {
        String clearQuery = clearString(query);
        String contentOfPage = Jsoup.parse(htmlContent).getElementsContainingOwnText(clearQuery).text();
        String clearTextOfPage = clearString(contentOfPage);
        String defaultTextOnPage = Jsoup.parse(htmlContent).body().text().substring(0, 150);

        int index = clearTextOfPage.indexOf(clearQuery);
        int maxCount = 0;
        int startIndex = 0;
        while (index >= 0) {
            int count = 1;
            int endIndex = index + clearQuery.length();
            while ((index = clearTextOfPage.indexOf(clearQuery, endIndex)) >= 0 && index - endIndex <= 50) {
                count++;
                endIndex = index + clearQuery.length();
            }

            if (count > maxCount) {
                maxCount = count;
                startIndex = Math.max(0, endIndex - 100);
            }
        }
        String snippet = clearTextOfPage.substring(startIndex, Math.min(clearTextOfPage.length(), startIndex + 200)) + "...";

        for (String queryWord : clearQuery.split("\\s+")) {
            index = snippet.indexOf(queryWord);
            while (index >= 0) {
                snippet = snippet.substring(0, index) + "<b>" + snippet.substring(index, index + queryWord.length()) + "</b>" + snippet.substring(index + queryWord.length());
                index = snippet.indexOf(queryWord, index + queryWord.length() + 7);
            }
        }
        return snippet.equals("...") ? defaultTextOnPage : snippet;
    }

    private static String clearString(String contentOfPage) {
        return contentOfPage
                .toLowerCase(Locale.ROOT)
                .replaceAll("([^а-яa-z])", " ");
    }

    private String getTitle(PageModel page) {
        String pageContent = page.getContent();
        int titleTextStartIndex = pageContent.indexOf("<title>");
        int titleTextEndIndex = pageContent.indexOf("</title>");

        if (titleTextStartIndex == -1 || titleTextEndIndex == -1) {
            return "";
        }

        titleTextStartIndex += 7;

        return pageContent.substring(titleTextStartIndex, titleTextEndIndex);
    }
}


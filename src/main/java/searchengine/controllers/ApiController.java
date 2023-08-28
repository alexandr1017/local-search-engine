package searchengine.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

import java.io.IOException;
import java.net.MalformedURLException;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService, SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() throws MalformedURLException {
        IndexingResponse indexingResponse = indexingService.startIndexing();
        return ResponseEntity.ok(indexingResponse);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        IndexingResponse indexingResponse = indexingService.stopIndexing();
        return ResponseEntity.ok(indexingResponse);
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(@RequestBody String path) throws IOException {
        IndexingResponse indexingResponse = indexingService.addPageToIndex(path);
        return ResponseEntity.ok(indexingResponse);
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(
            @RequestParam(name = "query") String query,
            @Value("${search-settings.offset}") int offset,
            @RequestParam(name = "limit", defaultValue = "20") int limit,
            @RequestParam(name = "site", defaultValue = "") String site
    ) throws IOException {
        SearchResponse searchResponse = searchService.search(query, offset, limit, site);
        return ResponseEntity.ok(searchResponse);
    }


}

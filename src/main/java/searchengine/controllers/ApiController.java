package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

import java.io.IOException;
import java.net.MalformedURLException;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }
    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing () throws MalformedURLException {
        IndexingResponse indexingResponse = indexingService.startIndexing();
        return ResponseEntity.ok(indexingResponse);
    }
    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing () throws InterruptedException {
        IndexingResponse indexingResponse = indexingService.stopIndexing();
        return ResponseEntity.ok(indexingResponse);
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage (@RequestBody String path) throws IOException {
        IndexingResponse indexingResponse = indexingService.addPageToIndex(path);
        return ResponseEntity.ok(indexingResponse);
    }


}

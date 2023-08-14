package searchengine.services;

import org.springframework.stereotype.Service;
import searchengine.dto.indexing.IndexingResponse;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;


public interface IndexingService {

    IndexingResponse startIndexing() throws MalformedURLException;

    IndexingResponse stopIndexing() throws InterruptedException;

    IndexingResponse addPageToIndex(String path) throws IOException;
}

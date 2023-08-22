package searchengine.services;

import searchengine.dto.indexing.IndexingResponse;

import java.io.IOException;
import java.net.MalformedURLException;


public interface IndexingService {

    IndexingResponse startIndexing() throws MalformedURLException;

    IndexingResponse stopIndexing();

    IndexingResponse addPageToIndex(String path) throws IOException;
}

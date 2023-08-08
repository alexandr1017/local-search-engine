package searchengine.services;

import org.springframework.stereotype.Service;
import searchengine.dto.indexing.IndexingResponse;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;


public interface IndexingService {

    public IndexingResponse startIndexing () throws MalformedURLException;
    public IndexingResponse stopIndexing () throws InterruptedException;
    public IndexingResponse addPageToIndex(String path) throws IOException;
}

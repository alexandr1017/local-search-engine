package searchengine.services;

import org.springframework.stereotype.Service;
import searchengine.dto.indexing.IndexingResponse;

import java.net.MalformedURLException;
import java.util.Map;


public interface IndexingService {

    public IndexingResponse startIndexing () throws MalformedURLException;
    public IndexingResponse stopIndexing ();
}

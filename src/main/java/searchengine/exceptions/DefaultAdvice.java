package searchengine.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.IndexingResponseFail;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchResponseNegative;

@ControllerAdvice
public class DefaultAdvice {
    @ExceptionHandler(IndexingAlreadyStartedException.class)
    public ResponseEntity<IndexingResponse> handleException(IndexingAlreadyStartedException e) {
        IndexingResponseFail response = new IndexingResponseFail(e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IncorrectURIException.class)
    public ResponseEntity<IndexingResponse> handleException(IncorrectURIException e) {
        IndexingResponseFail response = new IndexingResponseFail(e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SearchException.class)
    public ResponseEntity<SearchResponse> handleException(SearchException e) {
        SearchResponseNegative response = new SearchResponseNegative(e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

}

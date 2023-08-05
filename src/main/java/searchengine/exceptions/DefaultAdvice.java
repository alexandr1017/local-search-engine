package searchengine.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.IndexingResponseFail;

@ControllerAdvice
public class DefaultAdvice {
    @ExceptionHandler(IndexingAlreadyStartedException.class)
    public ResponseEntity<IndexingResponse> handleException(IndexingAlreadyStartedException e) {
        IndexingResponseFail response = new IndexingResponseFail(e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}

package searchengine.dto.indexing;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IndexingResponseFail extends IndexingResponse {
    private String error;
}

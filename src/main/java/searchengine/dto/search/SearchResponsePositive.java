package searchengine.dto.search;

import lombok.Data;

import java.util.List;

@Data
public class SearchResponsePositive extends SearchResponse {

    private int count;
    private List<SearchItem> data;

    public SearchResponsePositive(int count, List<SearchItem> data) {
        super(true);
        this.count = count;
        this.data = data;
    }
}

package searchengine.dto.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import searchengine.dto.statistics.DetailedStatisticsItem;

import java.util.List;

@Data
public class SearchResponsePositive extends SearchResponse{

    private int count;
    private List<SearchItem> data;

    public SearchResponsePositive (int count, List<SearchItem> data){
        super(true);
        this.count = count;
        this.data = data;
    }
}

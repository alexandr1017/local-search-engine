package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {

    @Query(nativeQuery = true,
            value = "select page_id from `index` where `index`.lemma_id=:lemmaId")
    List<Integer> findPageIdLemmaId(int lemmaId);

    @Query(nativeQuery = true,
            value = "SELECT * FROM search_engine.`index` i where i.page_id = :pageId AND i.lemma_id = :lemmaId")
    List<Index> findByPageIdAndLemmaId(int pageId, int lemmaId);
}

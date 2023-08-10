package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaModel;

import java.util.List;
@Repository
public interface LemmaRepository extends JpaRepository<LemmaModel,Integer> {

    @Query(nativeQuery = true,
            value = "select * from lemma where lemma.lemma=?1")
    List<LemmaModel> findByLemma(String lemma);

    @Query(nativeQuery = true,
            value = "select COUNT(*) from lemma where lemma.site_id=:siteId")
    Integer findCountOfLemmasBySiteId (Integer siteId);
    @Query(nativeQuery = true,
            value = "select COUNT(*) from lemma")
    Integer countOfLemmas ();
}

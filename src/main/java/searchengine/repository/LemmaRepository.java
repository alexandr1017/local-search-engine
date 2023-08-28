package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    @Query(nativeQuery = true,
            value = "select * from lemma where lemma.lemma=:lemma")
    List<Lemma> findByLemma(String lemma);

    @Query(nativeQuery = true,
            value = "select COUNT(*) from lemma where lemma.site_id=:siteId")
    Integer findCountOfLemmasBySiteId(Integer siteId);

    @Query(nativeQuery = true,
            value = "select * from lemma l where l.site_id=:siteId AND l.frequency <250 AND l.lemma=:lemma")
    Lemma customSelectFromLemmaDB(Integer siteId, String lemma);

    @Query(nativeQuery = true,
            value = "select * from lemma l where l.frequency <250 AND l.lemma=:lemma")
    Lemma customSelectAllSitesFromLemmaDB(String lemma);
}

package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Site;

import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {

    @Transactional
    void deleteByUrl(String url);

    Optional<Site> findByUrl(String url);

}

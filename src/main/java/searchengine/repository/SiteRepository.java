package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.SiteModel;

import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<SiteModel, Integer> {

    @Transactional
    void deleteByUrl(String url);

    Optional<SiteModel> findByUrl(String url);

}

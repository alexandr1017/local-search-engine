package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexModel;
import searchengine.model.SiteModel;


import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<SiteModel,Integer> {

    @Transactional
    void deleteByUrl(String url);

    Optional<SiteModel> findByUrl(String url);


}

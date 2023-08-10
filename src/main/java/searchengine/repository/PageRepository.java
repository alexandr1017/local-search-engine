package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<PageModel,Integer> {
    Optional<PageModel> findByPath(String path);

    @Query(nativeQuery = true,
            value = "select COUNT(*) from page where page.site_id=:siteId")
    Integer findCountOfPagesBySiteId (Integer siteId);
    @Query(nativeQuery = true,
            value = "select COUNT(*) from page")
    Integer countOfPages ();
}

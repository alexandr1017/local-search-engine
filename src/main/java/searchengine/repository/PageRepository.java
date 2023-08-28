package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    Optional<Page> findByPath(String path);

    @Query(nativeQuery = true,
            value = "select COUNT(*) from page where page.site_id=:siteId")
    Integer findCountOfPagesBySiteId(Integer siteId);

    @Query("SELECT p FROM Page p WHERE p.id IN :ids")
    List<Page> findPagesByIds(@Param("ids") List<Integer> ids);
}

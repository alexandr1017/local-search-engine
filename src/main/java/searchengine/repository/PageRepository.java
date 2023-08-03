package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageModel;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<PageModel,Integer> {
    Optional<PageModel> findByPath(String path);
}

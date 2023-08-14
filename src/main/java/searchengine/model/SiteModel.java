package searchengine.model;



import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Entity
@Table(name = "site")
public class SiteModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')", nullable = false)
    private String status;

    @Column(name = "status_time",nullable = false)
    private LocalDateTime statusTime;
    @Column(name = "last_error",columnDefinition = "TEXT")
    private String lastError;
    @Column(columnDefinition = "VARCHAR(255)",nullable = false)
    private String url;
    @Column(columnDefinition = "VARCHAR(255)",nullable = false, unique = true)
    private String name;

//    @OneToMany(mappedBy = "siteId",fetch = FetchType.LAZY)
//    private Set<PageModel> pageSet;
//    @OneToMany(mappedBy = "siteId",fetch = FetchType.LAZY)
//    private Set<LemmaModel> lemmaSet;

    public SiteModel (){
        setStatus("INDEXING");
        setStatusTime(LocalDateTime.now());
    }


}

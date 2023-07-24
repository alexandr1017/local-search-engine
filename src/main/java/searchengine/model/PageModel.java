package searchengine.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
@Table(name = "page", indexes = {@Index(name = "path_index", columnList = "`path`")})
public class PageModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteModel siteId;

    @Column(name = "path", columnDefinition = "VARCHAR(1000)", nullable = false)
    private String path;
    @Column(nullable = false)
    private Integer code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    @OneToMany(mappedBy = "pageId", fetch = FetchType.LAZY)
    private Set<IndexModel> indexSet;
}

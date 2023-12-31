package searchengine.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import javax.persistence.Index;

@Data
@NoArgsConstructor
@Entity
@Table(name = "page", indexes = {@Index(name = "siteId_path_index", columnList = "site_id, `path`", unique = true)})

public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "site_id", nullable = false)
    private Site siteId;

    @Column(name = "path", columnDefinition = "VARCHAR(1000)", nullable = false)
    private String path;
    @Column(nullable = false)
    private Integer code;

    @Column(columnDefinition = "MEDIUMTEXT CHARACTER SET utf8mb4", nullable = false)
    private String content;

}

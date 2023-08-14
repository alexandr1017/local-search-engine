package searchengine.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;


import javax.persistence.*;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
@Table(name = "lemma")
public class LemmaModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action= OnDeleteAction.CASCADE)
    @JoinColumn(name = "site_id",nullable = false)
    private SiteModel siteId;
    @Column(columnDefinition = "VARCHAR(255)",nullable = false)
    private String lemma;
    @Column(nullable = false)
    private Integer frequency;

//    @OneToMany(mappedBy = "lemmaId",fetch = FetchType.LAZY)
//    private Set<IndexModel> indexSet;

}

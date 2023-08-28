package searchengine.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;


import javax.persistence.*;

@Data
@NoArgsConstructor
@Entity
@Table(name = "lemma")
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "site_id", nullable = false)
    private Site siteId;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;
    @Column(nullable = false)
    private Integer frequency;

}

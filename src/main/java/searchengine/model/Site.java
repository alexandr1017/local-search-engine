package searchengine.model;


import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "site")
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')", nullable = false)
    private String status;

    @Column(name = "status_time", nullable = false)
    private LocalDateTime statusTime;
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String url;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false, unique = true)
    private String name;

    public Site() {
        setStatus("INDEXING");
        setStatusTime(LocalDateTime.now());
    }


}

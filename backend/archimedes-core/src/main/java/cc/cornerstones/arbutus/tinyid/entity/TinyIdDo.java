package cc.cornerstones.arbutus.tinyid.entity;

import lombok.Data;
import org.hibernate.annotations.Where;

import javax.persistence.*;

@Data
@Entity
@Table(name = TinyIdDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class TinyIdDo {
    public static final String RESOURCE_NAME = "t9_tiny_id";
    public static final String RESOURCE_SYMBOL = "Tiny ID";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "create_timestamp")
    private java.sql.Timestamp createTimestamp;

    @Column(name = "last_update_timestamp")
    private java.sql.Timestamp lastUpdateTimestamp;

    @Column(name = "biz_type")
    private String bizType;

    @Column(name = "begin_id")
    private Long beginId;

    @Column(name = "max_id")
    private Long maxId;

    private Integer step;

    private Integer delta;

    private Integer remainder;

    private Long version;
}
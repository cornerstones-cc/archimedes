package cc.cornerstones.arbutus.lock.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * Lock
 *
 * @author bbottong
 */

@Data
@Entity
@Table(name = LockDo.RESOURCE_NAME, indexes = {
        @Index(name="unique_name_resource_version", columnList = "name, resource, version", unique = true)
})
public class LockDo {
    public static final String RESOURCE_NAME = "t9_lock";
    public static final String RESOURCE_SYMBOL = "Lock";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "created_timestamp")
    private LocalDateTime createdTimestamp;

    @Column(name = "last_modified_timestamp")
    private LocalDateTime lastModifiedTimestamp;

    @Column(name = "name")
    private String name;

    @Column(name = "resource")
    private String resource;

    @Column(name = "version")
    private Long version;
}
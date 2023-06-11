package cc.cornerstones.almond.types;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * @author bbottong
 */
@Data
@MappedSuperclass
public abstract class BaseDo {
    public static final String ID = "id";
    public static final String ID_FIELD_NAME = "id";
    public static final String CREATED_TIMESTAMP = "created_timestamp";
    public static final String CREATED_TIMESTAMP_FIELD_NAME = "createdTimestamp";
    public static final String CREATED_BY = "created_by";
    public static final String CREATED_BY_FIELD_NAME = "createdBy";
    public static final String LAST_MODIFIED_BY = "last_modified_by";
    public static final String LAST_MODIFIED_BY_FIELD_NAME = "lastModifiedBy";
    public static final String LAST_MODIFIED_TIMESTAMP = "last_modified_timestamp";
    public static final String LAST_MODIFIED_TIMESTAMP_FIELD_NAME = "lastModifiedTimestamp";
    public static final String OWNER = "owner";
    public static final String OWNER_FIELD_NAME = "owner";
    public static final String DELETED = "is_deleted";
    public static final String DELETED_FIELD_NAME = "deleted";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = ID)
    private Long id;

    @Column(name = DELETED, columnDefinition = "boolean default false")
    private Boolean deleted;

    @Column(name = CREATED_TIMESTAMP)
    private LocalDateTime createdTimestamp;

    @Column(name = CREATED_BY)
    private Long createdBy;

    @Column(name = LAST_MODIFIED_TIMESTAMP)
    private LocalDateTime lastModifiedTimestamp;

    @Column(name = LAST_MODIFIED_BY)
    private Long lastModifiedBy;

    @Column(name = OWNER)
    private Long owner;

    public static void create(BaseDo object, Long author, LocalDateTime timestamp) {
        object.setCreatedBy(author);
        object.setCreatedTimestamp(timestamp);
        object.setLastModifiedTimestamp(timestamp);
        object.setLastModifiedBy(author);
        object.setDeleted(Boolean.FALSE);
        object.setOwner(author);
    }

    public static void create(BaseDo object, Long author, Long owner, LocalDateTime timestamp) {
        object.setCreatedBy(author);
        object.setCreatedTimestamp(timestamp);
        object.setLastModifiedTimestamp(timestamp);
        object.setLastModifiedBy(author);
        object.setDeleted(Boolean.FALSE);
        object.setOwner(owner);
    }

    public static void update(BaseDo object, Long author, LocalDateTime timestamp) {
        object.setLastModifiedBy(author);
        object.setLastModifiedTimestamp(timestamp);
    }

    public static void update(BaseDo object, Long author, Long owner, LocalDateTime timestamp) {
        object.setLastModifiedBy(author);
        object.setLastModifiedTimestamp(timestamp);
        object.setOwner(owner);
    }

    public static void logicallyDelete(BaseDo object, Long author, LocalDateTime timestamp) {
        object.setDeleted(Boolean.TRUE);
        object.setLastModifiedBy(author);
        object.setLastModifiedTimestamp(timestamp);
    }
}

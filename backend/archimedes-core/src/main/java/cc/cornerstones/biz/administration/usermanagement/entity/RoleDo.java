package cc.cornerstones.biz.administration.usermanagement.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Role
 *
 * @author bbottong
 */
@TinyId(bizType = RoleDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = RoleDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class RoleDo extends BaseDo {
    public static final String RESOURCE_NAME = "k2_role";
    public static final String RESOURCE_SYMBOL = "Role";

    /**
     * UID
     */
    @Column(name = "uid")
    private Long uid;

    /**
     * Name
     */
    @Column(name = "name", length = 129)
    private String name;

    /**
     * Object Name
     * <p>
     * An object name is how the object is referenced programmatically.
     */
    @Column(name = "object_name", length = 150)
    private String objectName;

    /**
     * Description
     * <p>
     * A meaning description helps you remembers the differences between objects.
     */
    @Column(name = "description", length = 255)
    private String description;

    /**
     * Sequence
     */
    @Column(name = "sequence")
    private Float sequence;

    /**
     * Directory
     */
    @Column(name = "is_directory")
    private Boolean directory;

    /**
     * Parent 的 UID
     */
    @Column(name = "parent_uid")
    private Long parentUid;
}
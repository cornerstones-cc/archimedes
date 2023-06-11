package cc.cornerstones.biz.app.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import cc.cornerstones.biz.app.share.constants.AppMembershipEnum;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.*;

/**
 * App data facet
 *
 * @author bbottong
 */

@TypeDef(name = "json", typeClass = JsonStringType.class)
@TinyId(bizType = AppDataFacetDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = AppDataFacetDo.RESOURCE_NAME,
        indexes = {
                @Index(name = "app_uid", columnList = "app_uid", unique = false)
        })
@Where(clause = "is_deleted=0")
public class AppDataFacetDo extends BaseDo {
    public static final String RESOURCE_NAME = "f1_app_data_facet";
    public static final String RESOURCE_SYMBOL = "App data facet";

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
     * Data facet's uid
     */
    @Column(name = "data_facet_uid")
    private Long dataFacetUid;

    /**
     * Parent 的 UID
     */
    @Column(name = "parent_uid")
    private Long parentUid;

    /**
     * App's UID
     */
    @Column(name = "app_uid")
    private Long appUid;
}
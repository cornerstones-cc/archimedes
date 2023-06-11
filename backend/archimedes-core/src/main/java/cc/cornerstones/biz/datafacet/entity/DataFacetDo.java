package cc.cornerstones.biz.datafacet.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import cc.cornerstones.biz.datafacet.share.constants.DataFacetVisibilityEnum;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.*;

/**
 * Data Facet
 *
 * @author bbottong
 */

@TypeDef(name = "json", typeClass = JsonStringType.class)
@TinyId(bizType = DataFacetDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = DataFacetDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class DataFacetDo extends BaseDo {
    public static final String RESOURCE_NAME = "a1_df_data_facet";
    public static final String RESOURCE_SYMBOL = "Data facet";

    /**
     * UID
     */
    @Column(name = "uid")
    private Long uid;

    /**
     * Name
     * <p>
     * A name is used to identify the object.
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
     * Remark
     */
    @Column(name = "remark", length = 255)
    private String remark;

    /**
     * 内部版本号
     */
    @Column(name = "build_number")
    private Long buildNumber;

    /**
     * 所属 Data Table 的 UID
     */
    @Column(name = "data_table_uid")
    private Long dataTableUid;

    /**
     * 所属 Data Source 的 UID
     */
    @Column(name = "data_source_uid")
    private Long dataSourceUid;
}
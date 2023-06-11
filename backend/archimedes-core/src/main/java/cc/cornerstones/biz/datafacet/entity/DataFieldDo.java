package cc.cornerstones.biz.datafacet.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import cc.cornerstones.biz.datafacet.share.constants.MeasurementRoleEnum;
import cc.cornerstones.biz.datafacet.share.constants.DataFieldTypeEnum;
import com.alibaba.fastjson.JSONObject;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.*;

/**
 * Data Field
 *
 * @author bbottong
 */
@TypeDef(name = "json", typeClass = JsonStringType.class)
@TinyId(bizType = DataFieldDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = DataFieldDo.RESOURCE_NAME,
        indexes = {
                @Index(name = "data_facet_uid", columnList = "data_facet_uid", unique = false)
        })
@Where(clause = "is_deleted=0")
public class DataFieldDo extends BaseDo {
    public static final String RESOURCE_NAME = "a1_df_data_field";
    public static final String RESOURCE_SYMBOL = "Data field";

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
     * Label
     * <p>
     * An label is an additional name for better reading
     */
    @Column(name = "label", length = 129)
    private String label;

    /**
     * Label (Physical)
     * <p>
     * An label is an additional name for better reading
     */
    @Column(name = "label_physical", length = 129)
    private String labelPhysical;

    /**
     * Label (Logical)
     * <p>
     * An label is an additional name for better reading
     */
    @Column(name = "label_logical", length = 129)
    private String labelLogical;

    /**
     * Description
     * <p>
     * A meaning description helps you remembers the differences between objects.
     */
    @Column(name = "description", length = 255)
    private String description;

    /**
     * Description (Physical)
     * <p>
     * A meaning description helps you remembers the differences between objects.
     */
    @Column(name = "description_physical", length = 255)
    private String descriptionPhysical;

    /**
     * Description (Logical)
     * <p>
     * A meaning description helps you remembers the differences between objects.
     */
    @Column(name = "description_logical", length = 255)
    private String descriptionLogical;

    /**
     * Type
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "type")
    private DataFieldTypeEnum type;

    /**
     * Type (Physical)
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "type_physical")
    private DataFieldTypeEnum typePhysical;

    /**
     * Type (Logical)
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "type_logical")
    private DataFieldTypeEnum typeLogical;

    /**
     * Type Extension
     *
     */
    @Type(type = "json")
    @Column(name = "type_extension", columnDefinition = "json")
    private JSONObject typeExtension;

    /**
     * Measurement role
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "measurement_role")
    private MeasurementRoleEnum measurementRole;

    /**
     * Sequence
     *
     * 在所有字段中的序号（从0开始计数）
     */
    @Column(name = "sequence")
    private Float sequence;

    /**
     * Sequence (Physical)
     */
    @Column(name = "sequence_physical")
    private Float sequencePhysical;

    /**
     * Sequence (Logical)
     */
    @Column(name = "sequence_logical")
    private Float sequenceLogical;

    /**
     * 所属 Data Facet 的 UID
     */
    @Column(name = "data_facet_uid")
    private Long dataFacetUid;
}
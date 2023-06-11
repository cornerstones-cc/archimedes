package cc.cornerstones.biz.datawidget.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import cc.cornerstones.biz.datawidget.share.constants.DataWidgetTypeEnum;
import com.alibaba.fastjson.JSONObject;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.*;

/**
 * Data widget
 *
 * @author bbottong
 */
@TypeDef(name = "json", typeClass = JsonStringType.class)
@TinyId(bizType = DataWidgetDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = DataWidgetDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class DataWidgetDo extends BaseDo {
    public static final String RESOURCE_NAME = "a1_dw_data_widget";
    public static final String RESOURCE_SYMBOL = "Data widget";

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
     * Type
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "type")
    private DataWidgetTypeEnum type;

    /**
     * Remark
     */
    @Column(name = "remark", length = 255)
    private String remark;

    /**
     * Build characteristics
     */
    @Type(type = "json")
    @Column(name = "build_characteristics", columnDefinition = "json")
    private JSONObject buildCharacteristics;

    /**
     * 所属 Data Facet 的 UID
     */
    @Column(name = "data_facet_uid")
    private Long dataFacetUid;
}
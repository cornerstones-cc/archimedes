package cc.cornerstones.biz.datafacet.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import cc.cornerstones.biz.datafacet.share.constants.DataFieldTypeEnum;
import cc.cornerstones.biz.datafacet.share.constants.MeasurementRoleEnum;
import cc.cornerstones.biz.share.constants.FilteringTypeEnum;
import com.alibaba.fastjson.JSONObject;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.*;

/**
 * Filtering Data Field
 *
 * @author bbottong
 */
@TypeDef(name = "json", typeClass = JsonStringType.class)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = FilteringDataFieldDo.RESOURCE_NAME,
        indexes = {
                @Index(name = "data_facet_uid", columnList = "data_facet_uid", unique = false)
        })
@Where(clause = "is_deleted=0")
public class FilteringDataFieldDo extends BaseDo {
    public static final String RESOURCE_NAME = "a1_df_appearance_filtering_data_field";
    public static final String RESOURCE_SYMBOL = "Filtering data field";

    /**
     * Field Name
     */
    @Column(name = "field_name", length = 129)
    private String fieldName;

    /**
     * Filtering type
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "filtering_type")
    private FilteringTypeEnum filteringType;

    /**
     * Filtering type extension
     */
    @Type(type = "json")
    @Column(name = "filtering_type_extension", columnDefinition = "json")
    private JSONObject filteringTypeExtension;

    /**
     * Default value settings
     */
    @Type(type = "json")
    @Column(name = "default_value_settings", columnDefinition = "json")
    private JSONObject defaultValueSettings;

    /**
     * Filtering sequence
     * <p>
     * 在所有 Filtering 字段中的序号（从0开始计数）
     */
    @Column(name = "filtering_sequence")
    private Float filteringSequence;

    /**
     * 所属 Data Facet 的 UID
     */
    @Column(name = "data_facet_uid")
    private Long dataFacetUid;
}
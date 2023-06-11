package cc.cornerstones.biz.datafacet.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.biz.share.constants.FilteringTypeEnum;
import com.alibaba.fastjson.JSONObject;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * Filtering Data Field
 *
 * @author bbottong
 */
@TypeDef(name = "json", typeClass = JsonStringType.class)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = ListingDataFieldDo.RESOURCE_NAME,
        indexes = {
                @Index(name = "data_facet_uid", columnList = "data_facet_uid", unique = false)
        })
@Where(clause = "is_deleted=0")
public class ListingDataFieldDo extends BaseDo {
    public static final String RESOURCE_NAME = "a1_df_appearance_listing_data_field";
    public static final String RESOURCE_SYMBOL = "Listing data field";

    /**
     * Field Name
     */
    @Column(name = "field_name", length = 129)
    private String fieldName;

    /**
     * Width
     */
    @Column(name = "width")
    private Integer width;

    /**
     * Extension
     */
    @Type(type = "json")
    @Column(name = "extension", columnDefinition = "json")
    private JSONObject extension;

    /**
     * Listing sequence
     * <p>
     * 在所有 Listing 字段中的序号（从0开始计数）
     */
    @Column(name = "listing_sequence")
    private Float listingSequence;

    /**
     * 所属 Data Facet 的 UID
     */
    @Column(name = "data_facet_uid")
    private Long dataFacetUid;
}
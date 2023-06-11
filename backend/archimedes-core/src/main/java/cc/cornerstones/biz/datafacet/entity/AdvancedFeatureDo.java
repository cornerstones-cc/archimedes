package cc.cornerstones.biz.datafacet.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import cc.cornerstones.biz.datafacet.dto.AdvancedFeatureContentDto;
import cc.cornerstones.biz.datafacet.dto.DataPermissionContentDto;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Advanced feature
 *
 * @author bbottong
 */
@TypeDef(name = "json", typeClass = JsonStringType.class)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = AdvancedFeatureDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class AdvancedFeatureDo extends BaseDo {
    public static final String RESOURCE_NAME = "a1_df_advanced_feature";
    public static final String RESOURCE_SYMBOL = "Advanced feature";

    /**
     * Content
     *
     */
    @Type(type = "json")
    @Column(name = "content", columnDefinition = "json")
    private AdvancedFeatureContentDto content;

    /**
     * 所属 Data Facet 的 UID
     */
    @Column(name = "data_facet_uid")
    private Long dataFacetUid;
}
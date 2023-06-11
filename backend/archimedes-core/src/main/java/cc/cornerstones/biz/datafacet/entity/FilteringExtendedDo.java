package cc.cornerstones.biz.datafacet.entity;

import cc.cornerstones.almond.types.BaseDo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Filtering extended
 *
 * @author bbottong
 */
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = FilteringExtendedDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class FilteringExtendedDo extends BaseDo {
    public static final String RESOURCE_NAME = "a1_df_appearance_filtering_extended";
    public static final String RESOURCE_SYMBOL = "Filtering extended";

    /**
     * Enabled default query
     */
    @Column(name = "is_enabled_default_query", columnDefinition = "boolean default false")
    private Boolean enabledDefaultQuery;

    /**
     * Enabled filter folding
     */
    @Column(name = "is_enabled_filter_folding", columnDefinition = "boolean default false")
    private Boolean enabledFilterFolding;

    /**
     * 所属 Data Facet 的 UID
     */
    @Column(name = "data_facet_uid")
    private Long dataFacetUid;
}
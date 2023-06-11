package cc.cornerstones.biz.datafacet.dto;

import cc.cornerstones.almond.types.BaseDto;
import lombok.Data;


@Data
public class FilteringExtendedDto extends BaseDto {

    /**
     * Enabled default query
     */
    private Boolean enabledDefaultQuery;

    /**
     * Enabled filter folding
     */
    private Boolean enabledFilterFolding;

    /**
     * 所属 Data Facet 的 UID
     */
    private Long dataFacetUid;
}

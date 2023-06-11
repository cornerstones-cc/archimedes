package cc.cornerstones.biz.datafacet.dto;

import cc.cornerstones.almond.types.BaseDto;
import lombok.Data;
import org.springframework.data.domain.Sort;

@Data
public class SortingDataFieldDto extends BaseDto {

    /**
     * Field Name
     */
    private String fieldName;

    /**
     * Sorting direction
     */
    private Sort.Direction direction;

    /**
     * Sorting sequence
     * <p>
     * 在所有 Sorting 字段中的序号（从0开始计数）
     */
    private Float sortingSequence;

    /**
     * 所属 Data Facet 的 UID
     */
    private Long dataFacetUid;
}

package cc.cornerstones.biz.datafacet.dto;

import cc.cornerstones.almond.types.BaseDto;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class ListingDataFieldDto extends BaseDto {

    /**
     * Field Name
     */
    private String fieldName;

    /**
     * Width
     */
    private Integer width;

    /**
     * Extension
     */
    private JSONObject extension;

    /**
     * Listing sequence
     * <p>
     * 在所有 Listing 字段中的序号（从0开始计数）
     */
    private Float listingSequence;

    /**
     * 所属 Data Facet 的 UID
     */
    private Long dataFacetUid;
}

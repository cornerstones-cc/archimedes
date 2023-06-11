package cc.cornerstones.biz.datafacet.dto;

import cc.cornerstones.almond.types.BaseDto;
import cc.cornerstones.biz.share.constants.FilteringTypeEnum;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class FilteringDataFieldAnotherDto extends BaseDto {

    /**
     * Field Name
     */
    private String fieldName;

    /**
     * Filtering type
     */
    private FilteringTypeEnum filteringType;

    /**
     * Filtering type extension
     */
    private Object filteringTypeExtension;

    /**
     * Default value settings
     */
    private Object defaultValueSettings;

    /**
     * Filtering sequence
     * <p>
     * 在所有 Filtering 字段中的序号（从0开始计数）
     */
    private Float filteringSequence;

    /**
     * 所属 Data Facet 的 UID
     */
    private Long dataFacetUid;
}

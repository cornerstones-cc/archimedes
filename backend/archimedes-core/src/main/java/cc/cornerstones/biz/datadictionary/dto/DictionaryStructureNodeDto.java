package cc.cornerstones.biz.datadictionary.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * Dictionary Structure (字典结构)
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class DictionaryStructureNodeDto {
    private Long uid;
    private String name;
    private String description;

    /**
     * 字典结构中上一级节点的 UID
     */
    private Long parentUid;

    /**
     * 指定字典类目的 UID
     */
    private Long dictionaryCategoryUid;
}

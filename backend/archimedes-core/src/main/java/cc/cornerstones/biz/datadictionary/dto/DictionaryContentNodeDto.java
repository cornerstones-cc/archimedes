package cc.cornerstones.biz.datadictionary.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class DictionaryContentNodeDto {
    /**
     * 内容节点的 UID
     */
    private Long uid;

    /**
     * 内容节点的 Value
     */
    private String value;

    /**
     * 内容节点的 Symbol
     */
    private String symbol;

    /**
     * 内容树中上一级节点的UID
     */
    private Long parentUid;

    /**
     * 指定字典结构节点的UID
     */
    private Long dictionaryStructureNodeUid;

    /**
     * 指定字典类目的UID
     */
    private Long dictionaryCategoryUid;
}

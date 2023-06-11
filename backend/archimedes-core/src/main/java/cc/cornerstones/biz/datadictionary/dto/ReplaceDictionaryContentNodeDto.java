package cc.cornerstones.biz.datadictionary.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ReplaceDictionaryContentNodeDto {
    /**
     * 内容节点的 Value
     */
    @Size(min = 0, max = 255,
            message = "The description cannot exceed 255 characters in length")
    private String value;

    /**
     * 内容节点的 Symbol
     */
    @NotBlank(message = "symbol is required")
    @Size(min = 0, max = 255,
            message = "The description cannot exceed 255 characters in length")
    private String symbol;

    /**
     * 内容树中上一级节点的UID
     */
    private Long parentUid;

    /**
     * 指定字典结构节点的UID
     */
    @NotNull(message = "dictionary_structure_node_uid is required")
    private Long dictionaryStructureNodeUid;

    /**
     * 指定字典类目的UID
     */
    @NotNull(message = "dictionary_category_uid is required")
    private Long dictionaryCategoryUid;
}

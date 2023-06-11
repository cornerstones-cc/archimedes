package cc.cornerstones.biz.datadictionary.dto;

import cc.cornerstones.biz.datatable.share.constants.DictionaryBuildTypeEnum;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import javax.validation.constraints.NotNull;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class UpdateDictionaryBuildDto {
    /**
     * 是否启用该流程
     */
    @NotNull(message = "enabled is required")
    private Boolean enabled;

    /**
     * CRON Expression
     */
    @NotNull(message = "cron_expression is required")
    private String cronExpression;

    /**
     * 构建类型
     */
    private DictionaryBuildTypeEnum type;

    /**
     * 构建逻辑
     */
    private JSONObject logic;

}

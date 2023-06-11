package cc.cornerstones.biz.distributedtask.dto;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class CreateDistributedTaskDto {
    /**
     * Name
     */
    private String name;

    /**
     * Payload
     */
    private JSONObject payload;

    /**
     * Type
     */
    private String type;

    /**
     * Handler Name
     */
    private String handlerName;
}

package cc.cornerstones.biz.administration.serviceconnection.dto;

import cc.cornerstones.arbutus.pf4j.share.types.PluginProfile;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class BackEndComponentParsedResultDto {
    private PluginProfile metadata;
    private String configurationTemplate;

    /**
     * Only for authentication service provider
     */
    private JSONObject userInfoSchema;
}

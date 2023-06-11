package cc.cornerstones.biz.settings.dto;

import cc.cornerstones.biz.administration.serviceconnection.share.constants.ServiceComponentTypeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class SignInOptionDto {
    private Long uid;
    private String name;
    private String objectName;
    private String description;

    /**
     * Preferred
     */
    private Boolean preferred;

    /**
     * Type
     */
    private ServiceComponentTypeEnum type;

    /**
     * Front-end component
     */
    private String frontEndComponent;
}

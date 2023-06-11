package cc.cornerstones.biz.datatable.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * Data Column (Simple information)
 *
 * @author bbottong
 *
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class DataColumnSimpleDto {

    private Long uid;

    /**
     * Name
     * <p>
     * A name is used to identify the object.
     */
    private String name;

    /**
     * Object Name
     * <p>
     * An object name is how the object is referenced programmatically.
     */
    private String objectName;
}
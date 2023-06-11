package cc.cornerstones.biz.datasource.dto;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class UpdateDataSourceDto {
    /**
     * Name
     * <p>
     * A name is used to identify the object.
     */
    private String name;

    /**
     * Description
     *
     * A meaning description helps you remembers the differences between objects.
     */
    private String description;

    /**
     * Connection profile of the database server
     */
    private JSONObject connectionProfile;
}

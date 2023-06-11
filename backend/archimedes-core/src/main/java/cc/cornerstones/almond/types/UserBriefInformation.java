package cc.cornerstones.almond.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class UserBriefInformation {
    private Long uid;
    private String displayName;

    /**
     * extended property list
     *
     * f --- extended property uid
     * s --- extended property name
     * t --- extended property value
     */
    private List<AbcTuple3<Long, String, Object>> extendedPropertyList;
}

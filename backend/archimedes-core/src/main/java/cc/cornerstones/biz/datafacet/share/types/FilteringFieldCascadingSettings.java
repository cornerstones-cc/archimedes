package cc.cornerstones.biz.datafacet.share.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;


@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class FilteringFieldCascadingSettings {
    private List<String> fields;
    private Long dictionaryCategoryUid;
    private String filterName;
    private String filterLabel;
    private String filterDescription;
}

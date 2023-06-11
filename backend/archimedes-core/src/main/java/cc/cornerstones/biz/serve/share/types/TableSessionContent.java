package cc.cornerstones.biz.serve.share.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import org.springframework.data.domain.Sort;

import java.util.List;


@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class TableSessionContent {
    private List<FilteringField> filteringFieldList;
    private List<SortingField> sortingFieldList;
    private List<ListingField> listingFieldList;

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    private static class FilteringField {
        private String fieldName;
        private List<String> fieldValues;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    private static class ListingField {
        private String fieldName;
        private Integer width;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    private static class SortingField {
        private String fieldName;
        private Sort.Direction direction;
    }
}

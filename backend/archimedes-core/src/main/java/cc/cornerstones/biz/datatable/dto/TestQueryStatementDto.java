package cc.cornerstones.biz.datatable.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import javax.validation.constraints.NotNull;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class TestQueryStatementDto {
    /**
     * Query Statement
     */
    @NotNull(message = "query_statement is required")
    private String queryStatement;

    /**
     * Limit
     */
    private Integer limit = 10;
}

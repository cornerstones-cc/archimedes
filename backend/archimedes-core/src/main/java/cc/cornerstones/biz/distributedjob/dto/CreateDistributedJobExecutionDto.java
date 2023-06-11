package cc.cornerstones.biz.distributedjob.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import javax.validation.constraints.Size;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class CreateDistributedJobExecutionDto {
    /**
     * Description
     *
     * A meaning description helps you remembers the differences between objects.
     */
    @Size(min = 0, max = 255,
            message = "The description cannot exceed 255 characters in length")
    private String description;

}

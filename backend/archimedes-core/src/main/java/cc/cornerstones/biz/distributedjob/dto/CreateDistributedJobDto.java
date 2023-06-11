package cc.cornerstones.biz.distributedjob.dto;

import cc.cornerstones.biz.distributedjob.share.constants.JobExecutorRoutingAlgorithmEnum;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class CreateDistributedJobDto {
    /**
     * Name
     *
     * A name is used to identify the object.
     */
    @NotBlank(message = "name is required")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5_a-zA-Z0-9\\s-]+$", message = "Only Chinese characters, or English letters, or numbers, or spaces, or underscores, or hyphens are allowed")
    @Size(min = 1, max = 64,
            message = "The name cannot exceed 64 characters in length")
    private String name;

    /**
     * Description
     *
     * A meaning description helps you remembers the differences between objects.
     */
    @Size(min = 0, max = 255,
            message = "The description cannot exceed 255 characters in length")
    private String description;

    /**
     * Cron expression for scheduling job
     */
    @NotBlank(message = "cron_expression is required")
    @Size(min = 0, max = 45,
            message = "It cannot exceed 45 characters in length")
    private String cronExpression;

    /**
     * Algorithm for routing jobs to job executors
     */
    private JobExecutorRoutingAlgorithmEnum routingAlgorithm;

    /**
     * The name of the job handler on the job executor
     */
    @NotBlank(message = "handler_name is required")
    @Size(min = 0, max = 255,
            message = "The description cannot exceed 255 characters in length")
    private String handlerName;

    /**
     * Each time the job is scheduled to execute, the input parameters of the job handler
     */
    private JSONObject parameters;

    /**
     * Job execution timeout duration in seconds
     */
    private Long timeoutDurationInSecs = 0L;

    /**
     * The number of failed retries for the job
     */
    private Integer failedRetires = 0;

    /**
     * Enabled
     */
    private Boolean enabled = false;
}

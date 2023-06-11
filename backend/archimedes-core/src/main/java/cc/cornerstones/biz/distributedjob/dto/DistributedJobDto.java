package cc.cornerstones.biz.distributedjob.dto;

import cc.cornerstones.almond.types.BaseDto;
import cc.cornerstones.biz.distributedjob.share.constants.JobExecutorRoutingAlgorithmEnum;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import javax.persistence.Column;
import java.time.LocalDateTime;

@Data
public class DistributedJobDto extends BaseDto {
    private Long uid;

    /**
     * Name
     *
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
     * Cron expression for scheduling job
     */
    private String cronExpression;

    /**
     * Last execution timestamp
     */
    private LocalDateTime lastExecutionTimestamp;

    /**
     * Last executor hostname
     */
    private String lastExecutorHostname;

    /**
     * Last executor ip address
     */
    private String lastExecutorIpAddress;

    /**
     * Next execution timestamp according to cron expression
     */
    private LocalDateTime nextExecutionTimestamp;

    /**
     * Algorithm for routing jobs to job executors
     */
    private JobExecutorRoutingAlgorithmEnum routingAlgorithm;

    /**
     * The name of the job handler on the job executor
     */
    private String handlerName;

    /**
     * Each time the job is scheduled to execute, the input parameters of the job handler
     */
    private JSONObject parameters;

    /**
     * Job execution timeout duration in seconds
     */
    private Long timeoutDurationInSecs;

    /**
     * Enabled
     */
    private Boolean enabled = false;
}

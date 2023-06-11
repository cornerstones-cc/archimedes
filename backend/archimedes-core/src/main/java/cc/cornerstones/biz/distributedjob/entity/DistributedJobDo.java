package cc.cornerstones.biz.distributedjob.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import cc.cornerstones.biz.distributedjob.share.constants.JobExecutorRoutingAlgorithmEnum;
import com.alibaba.fastjson.JSONObject;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Job
 *
 * @author bbottong
 */
@TypeDef(name = "json", typeClass = JsonStringType.class)
@TinyId(bizType = DistributedJobDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = DistributedJobDo.RESOURCE_NAME,
        indexes = {
                @Index(name = "enabled_n_next_execution_timestamp", columnList = "enabled, next_execution_timestamp", unique = false)
        })
@Where(clause = "is_deleted=0")
public class DistributedJobDo extends BaseDo {
    public static final String RESOURCE_NAME = "t7_job";
    public static final String RESOURCE_SYMBOL = "Job";

    /**
     * UID
     */
    @Column(name = "uid")
    private Long uid;

    /**
     * Name
     * <p>
     * A name is used to identify the object.
     */
    @Column(name = "name", length = 129)
    private String name;

    /**
     * Object Name
     * <p>
     * An object name is how the object is referenced programmatically.
     */
    @Column(name = "object_name", length = 150)
    private String objectName;

    /**
     * Description
     * <p>
     * A meaning description helps you remembers the differences between objects.
     */
    @Column(name = "description", length = 255)
    private String description;

    /**
     * Cron expression for scheduling job
     */
    @Column(name = "cron_expression", length = 45)
    private String cronExpression;

    /**
     * Last execution timestamp
     */
    @Column(name = "last_execution_timestamp")
    private LocalDateTime lastExecutionTimestamp;

    /**
     * Last executor hostname
     */
    @Column(name = "last_executor_hostname", length = 255)
    private String lastExecutorHostname;

    /**
     * Last executor ip address
     */
    @Column(name = "last_executor_ip_address", length = 64)
    private String lastExecutorIpAddress;

    /**
     * Next execution timestamp according to cron expression
     */
    @Column(name = "next_execution_timestamp")
    private LocalDateTime nextExecutionTimestamp;

    /**
     * Algorithm for routing jobs to job executors
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "routing_algorithm")
    private JobExecutorRoutingAlgorithmEnum routingAlgorithm;

    /**
     * The name of the job handler on the job executor
     */
    @Column(name = "handler_name", length = 255)
    private String handlerName;

    /**
     * Each time the job is scheduled to execute, the input parameters of the job handler
     */
    @Type(type = "json")
    @Column(name = "parameters", columnDefinition = "json")
    private JSONObject parameters;

    /**
     * Job execution timeout duration in seconds
     */
    @Column(name = "timeout_duration_in_secs")
    private Long timeoutDurationInSecs;

    /**
     * The number of failed retries for the job
     */
    @Column(name = "failed_retries")
    private Integer failedRetires;

    /**
     * Enabled
     */
    @Column(name = "enabled", columnDefinition = "boolean default false")
    private Boolean enabled;

    /**
     * Hashed string
     */
    @Column(name = "hashed_string", length = 255)
    private String hashedString;
}
package cc.cornerstones.biz.distributedjob.entity;

import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Distributed job execution
 *
 * @author bbottong
 */
@TypeDef(name = "json", typeClass = JsonStringType.class)
@TinyId(bizType = DistributedJobExecutionDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0,
        delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = DistributedJobExecutionDo.RESOURCE_NAME,
        indexes = {
                @Index(name = "job_uid_n_status", columnList = "job_uid, status", unique = false),
                @Index(name = "executor_hostname_n_executor_ip_address", columnList = "executor_hostname, " +
                        "executor_ip_address", unique = false),
                @Index(name = "created_timestamp", columnList = "created_timestamp", unique = false),
        })
@Where(clause = "is_deleted=0")
public class DistributedJobExecutionDo extends BaseDo {
    public static final String RESOURCE_NAME = "t7_job_execution";
    public static final String RESOURCE_SYMBOL = "Distributed job execution";

    /**
     * UID
     */
    @Column(name = "uid")
    private Long uid;

    /**
     * Status
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status")
    private JobStatusEnum status;

    /**
     * Remark
     */
    @Column(name = "remark", length = 255)
    private String remark;

    /**
     * Begin timestamp
     */
    @Column(name = "begin_timestamp")
    private LocalDateTime beginTimestamp;

    /**
     * End timestamp
     */
    @Column(name = "end_timestamp")
    private LocalDateTime endTimestamp;

    /**
     * Hostname of the underlying executor
     */
    @Column(name = "executor_hostname", length = 128)
    private String executorHostname;

    /**
     * IP Address of the underlying executor
     */
    @Column(name = "executor_ip_address", length = 45)
    private String executorIpAddress;

    /**
     * Distributed job UID
     */
    @Column(name = "job_uid")
    private Long jobUid;
}
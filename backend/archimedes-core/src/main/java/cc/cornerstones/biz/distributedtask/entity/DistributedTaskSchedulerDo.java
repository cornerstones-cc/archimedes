package cc.cornerstones.biz.distributedtask.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * Distributed Task Scheduler
 *
 * @author bbottong
 */
@TypeDef(name = "json", typeClass = JsonStringType.class)
@TinyId(bizType = DistributedTaskSchedulerDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0,
        delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = DistributedTaskSchedulerDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class DistributedTaskSchedulerDo extends BaseDo {
    public static final String RESOURCE_NAME = "t7_task_scheduler";
    public static final String RESOURCE_SYMBOL = "Distributed task scheduler";

    /**
     * UID
     */
    @Column(name = "uid")
    private Long uid;

    /**
     * Hostname
     */
    @Column(name = "hostname", length = 255)
    private String hostname;

    /**
     * IP Address
     */
    @Column(name = "ip_address", length = 255)
    private String ipAddress;

    /**
     * Effective to
     */
    @Column(name = "is_effective")
    private Boolean effective;
    
    /**
     * Effective from
     */
    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    /**
     * Effective to
     */
    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;
}
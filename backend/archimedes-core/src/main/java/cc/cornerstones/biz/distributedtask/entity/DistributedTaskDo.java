package cc.cornerstones.biz.distributedtask.entity;

import cc.cornerstones.almond.constants.TaskStatusEnum;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
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
 * Distributed Task
 *
 * @author bbottong
 */
@TypeDef(name = "json", typeClass = JsonStringType.class)
@TinyId(bizType = DistributedTaskDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = DistributedTaskDo.RESOURCE_NAME,
        indexes = {
                @Index(name = "uid_n_status", columnList = "uid, status", unique = false)
        })
@Where(clause = "is_deleted=0")
public class DistributedTaskDo extends BaseDo {
    public static final String RESOURCE_NAME = "t7_task";
    public static final String RESOURCE_SYMBOL = "Distributed task";

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
     * Payload
     */
    @Type(type = "json")
    @Column(name = "payload", columnDefinition = "json")
    private JSONObject payload;

    /**
     * Type
     */
    @Column(name = "type", length = 255)
    private String type;

    /**
     * The name of the task handler on the task executor
     */
    @Column(name = "handler_name", length = 255)
    private String handlerName;

    /**
     * Status
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status")
    private TaskStatusEnum status;

    /**
     * Remark
     */
    @Column(name = "remark", length = 255)
    private String remark;

    /**
     * 开始时间戳
     */
    @Column(name = "begin_timestamp")
    private LocalDateTime beginTimestamp;

    /**
     * 结束时间戳
     */
    @Column(name = "end_timestamp")
    private LocalDateTime endTimestamp;

    /**
     * Hostname of the underlying executor
     */
    @Column(name = "executor_hostname", length = 255)
    private String executorHostname;

    /**
     * IP Address of the underlying executor
     */
    @Column(name = "executor_ip_address", length = 45)
    private String executorIpAddress;
}
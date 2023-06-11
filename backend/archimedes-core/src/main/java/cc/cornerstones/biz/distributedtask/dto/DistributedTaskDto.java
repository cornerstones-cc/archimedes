package cc.cornerstones.biz.distributedtask.dto;

import cc.cornerstones.almond.constants.TaskStatusEnum;
import cc.cornerstones.almond.types.BaseDto;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Distributed Task
 *
 * @author bbottong
 */
@Data
public class DistributedTaskDto extends BaseDto {
    /**
     * UID
     */
    private Long uid;

    /**
     * Name
     * <p>
     * A name is used to identify the object.
     */
    private String name;

    /**
     * Payload
     */
    private JSONObject payload;

    /**
     * The name of the task handler on the task executor
     */
    private String handlerName;

    /**
     * Status
     */
    private TaskStatusEnum status;

    /**
     * Remark
     */
    private String remark;

    /**
     * 开始时间戳
     */
    private LocalDateTime beginTimestamp;

    /**
     * 结束时间戳
     */
    private LocalDateTime endTimestamp;

    /**
     * Hostname of the underlying executor
     */
    private String executorHostname;

    /**
     * IP Address of the underlying executor
     */
    private String executorIpAddress;

    private String totalDuration;

    private String executionDuration;
}
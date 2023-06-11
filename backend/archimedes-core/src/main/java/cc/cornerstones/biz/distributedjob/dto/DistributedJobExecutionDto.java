package cc.cornerstones.biz.distributedjob.dto;

import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.types.BaseDto;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DistributedJobExecutionDto extends BaseDto {
    /**
     * UID
     */
    private Long uid;

    /**
     * Status
     */
    private JobStatusEnum status;

    /**
     * Remark
     */
    private String remark;

    /**
     * Begin timestamp
     */
    private LocalDateTime beginTimestamp;

    /**
     * End timestamp
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

    /**
     * Distributed job UID
     */
    private Long jobUid;

    private String totalDuration;

    private String executionDuration;
}

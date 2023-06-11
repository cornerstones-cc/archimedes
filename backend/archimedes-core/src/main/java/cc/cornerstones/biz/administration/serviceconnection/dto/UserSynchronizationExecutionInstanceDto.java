package cc.cornerstones.biz.administration.serviceconnection.dto;

import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.types.BaseDto;
import lombok.Data;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.time.LocalDateTime;

@Data
public class UserSynchronizationExecutionInstanceDto extends BaseDto {

    /**
     * UID
     */
    private Long uid;

    /**
     * Status
     */
    private JobStatusEnum status;

    /**
     * Begin timestamp
     */
    private LocalDateTime beginTimestamp;

    /**
     * End timestamp
     */
    private LocalDateTime endTimestamp;

    /**
     * Remark
     */
    private String remark;

    /**
     * Summary
     */
    private UserSynchronizationExecutionSummaryDto summary;

    /**
     * Service agent uid
     */
    private Long serviceAgentUid;

    private String totalDuration;

    private String executionDuration;
}

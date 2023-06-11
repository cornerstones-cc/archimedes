package cc.cornerstones.biz.datadictionary.dto;

import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.types.BaseDto;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DictionaryBuildInstanceDto extends BaseDto {
    /**
     * 实例的 UID
     */
    private Long uid;

    /**
     * 实例的执行状态
     */
    private JobStatusEnum status;

    /**
     * 实例的创建时间戳
     */
    private LocalDateTime createdTimestamp;

    /**
     * 实例的开始时间戳
     */
    private LocalDateTime startedTimestamp;

    /**
     * 实例的完成时间戳
     */
    private LocalDateTime finishedTimestamp;

    /**
     * 实例的失败时间戳
     */
    private LocalDateTime failedTimestamp;

    /**
     * 实例的取消时间戳
     */
    private LocalDateTime canceledTimestamp;
}

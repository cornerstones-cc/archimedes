package cc.cornerstones.biz.administration.systemsettings.dto;

import cc.cornerstones.almond.types.BaseDto;
import lombok.Data;

@Data
public class ArchiveCleanupSettingsDto extends BaseDto {
    /**
     * Whether enabled cleanup
     */
    private Boolean enabledCleanup;

    /**
     * A regular expression of the start time of each cleanup
     */
    private String startTimeCronExpression;

    /**
     * How long (in minutes) a cleanup lasts at most
     */
    private Integer maximumDurationInMinutes;

    /**
     * The maximum number of days that data rows marked as logical delete are retained
     */
    private Integer maximumRetainDaysForLogicalDelete;

    /**
     * The maximum number of days that access logs are retained
     */
    private Integer maximumRetainDaysForAccessLogs;

    /**
     * The maximum number of days that job execution logs are retained
     */
    private Integer maximumRetainDaysForJobExecutionLogs;

    /**
     * The maximum number of days that task execution logs are retained
     */
    private Integer maximumRetainDaysForTaskExecutionLogs;

    /**
     * The maximum number of days that export logs are retained
     */
    private Integer maximumRetainDaysForExportLogs;

    /**
     * The maximum number of days that dictionary build logs are retained
     */
    private Integer maximumRetainDaysForDictionaryBuildLogs;
}

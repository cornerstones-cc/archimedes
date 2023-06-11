package cc.cornerstones.biz.datafacet.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class AdvancedFeatureContentDto {
    private Boolean enabledHeader;
    private Boolean enabledTable;
    private Boolean enabledPivotTable;
    private Boolean enabledChart;
    private Boolean enabledMaintenanceWindow;
    private List<MaintenanceWindow> maintenanceWindowList;

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    public static class MaintenanceWindow {
        /**
         * Start time (cron expression)
         */
        private String cronExpression;

        /**
         * Duration in minutes
         */
        private Integer durationInMinutes;

        /**
         * Prompt message
         */
        private String message;
    }
}

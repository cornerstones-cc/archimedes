package cc.cornerstones.biz.administration.systemsettings.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class CreateOrReplaceCapacityControlSettingsDto {
    private Integer maximumNumberOfImagesExportedByOneExportTask;

    private Integer maximumNumberOfFilesExportedByOneExportTask;
}

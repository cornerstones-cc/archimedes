package cc.cornerstones.biz.administration.systemsettings.dto;

import cc.cornerstones.almond.types.BaseDto;
import lombok.Data;

@Data
public class CapacityControlSettingsDto extends BaseDto {
    private Integer maximumNumberOfImagesExportedByOneExportTask;

    private Integer maximumNumberOfFilesExportedByOneExportTask;
}

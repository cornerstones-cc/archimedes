package cc.cornerstones.biz.export.service.assembly;

import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.datasource.dto.DataSourceExportDto;
import cc.cornerstones.biz.datasource.dto.DataSourceQueryDto;
import lombok.Data;

import java.io.File;

@Data
public class ExportAsTemplatePayload {
    private Long dataSourceUid;
    private DataSourceExportDto dataSourceExport;
    private UserProfile operatingUserProfile;
    private File exportExtendedTemplateFile;
}

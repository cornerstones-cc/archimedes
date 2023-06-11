package cc.cornerstones.biz.export.service.assembly;

import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.datasource.dto.DataSourceExportDto;
import cc.cornerstones.biz.datasource.dto.DataSourceQueryDto;
import lombok.Data;

@Data
public class ExportExcelPayload {
    private Long dataSourceUid;
    private DataSourceExportDto dataSourceExport;
    private UserProfile operatingUserProfile;
}

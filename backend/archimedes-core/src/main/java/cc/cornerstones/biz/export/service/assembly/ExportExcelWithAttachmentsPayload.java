package cc.cornerstones.biz.export.service.assembly;

import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.datasource.dto.DataSourceExportDto;
import cc.cornerstones.biz.datasource.dto.DataSourceQueryDto;
import cc.cornerstones.biz.share.types.ExportAttachment;
import lombok.Data;

import java.util.List;

@Data
public class ExportExcelWithAttachmentsPayload {
    private Long dataSourceUid;
    private DataSourceExportDto dataSourceExport;
    private UserProfile operatingUserProfile;
    private List<ExportAttachment> fileAttachmentList;
    private List<ExportAttachment> imageAttachmentList;
}

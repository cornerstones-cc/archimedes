package cc.cornerstones.biz.datatable.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.datatable.dto.DataTableExportDto;
import cc.cornerstones.biz.datatable.dto.DataTableQueryDto;
import cc.cornerstones.biz.share.types.QueryContentResult;

public interface ExecuteDataTableService {
    QueryContentResult queryContent(
            Long dataTableUid,
            DataTableQueryDto dataTableQueryDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Long exportContent(
            String name,
            Long dataTableUid,
            DataTableExportDto dataTableExportDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}

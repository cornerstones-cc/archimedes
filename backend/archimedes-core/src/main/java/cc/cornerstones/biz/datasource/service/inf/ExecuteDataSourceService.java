package cc.cornerstones.biz.datasource.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.datasource.dto.DataSourceExportDto;
import cc.cornerstones.biz.datasource.dto.DataSourceQueryDto;
import cc.cornerstones.biz.export.share.constants.FileFormatEnum;
import cc.cornerstones.biz.share.types.QueryContentResult;
import com.alibaba.fastjson.JSONObject;

public interface ExecuteDataSourceService {
    QueryContentResult queryContent(
            Long dataSourceUid,
            DataSourceQueryDto dataSourceQueryDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Long exportContent(
            String name,
            Long dataSourceUid,
            DataSourceExportDto dataSourceExportDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}

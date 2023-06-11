package cc.cornerstones.biz.datafacet.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.datafacet.dto.DataFacetExportDto;
import cc.cornerstones.biz.datafacet.dto.DataFacetQueryDto;
import cc.cornerstones.biz.export.share.constants.FileFormatEnum;
import cc.cornerstones.biz.share.types.QueryContentResult;
import com.alibaba.fastjson.JSONObject;

public interface ExecuteDataFacetService {
    QueryContentResult queryContent(
            Long dataFacetUid,
            DataFacetQueryDto dataFacetQueryDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Long exportContent(
            Long dataFacetUid,
            DataFacetExportDto dataFacetExportDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}

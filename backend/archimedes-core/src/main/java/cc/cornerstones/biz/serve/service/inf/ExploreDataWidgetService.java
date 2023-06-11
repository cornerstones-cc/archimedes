package cc.cornerstones.biz.serve.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.datawidget.dto.DataWidgetDto;
import cc.cornerstones.biz.datawidget.share.constants.DataWidgetTypeEnum;
import cc.cornerstones.biz.share.types.QueryContentResult;
import com.alibaba.fastjson.JSONObject;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface ExploreDataWidgetService {
    List<DataWidgetDto> listingQueryDataWidgetsOfDataFacet(
            Long dataFacetUid,
            Long uid,
            String name,
            DataWidgetTypeEnum type,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    DataWidgetDto getDataWidget(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    QueryContentResult queryContent(
            Long uid,
            JSONObject request,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Long exportContent(
            Long uid,
            JSONObject request,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

}

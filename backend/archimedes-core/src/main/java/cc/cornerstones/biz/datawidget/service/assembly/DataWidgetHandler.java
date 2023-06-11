package cc.cornerstones.biz.datawidget.service.assembly;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.biz.datafacet.entity.DataFieldDo;
import cc.cornerstones.biz.datawidget.share.constants.DataWidgetTypeEnum;
import com.alibaba.fastjson.JSONObject;

import java.util.List;

public interface DataWidgetHandler {
    /**
     * Data widget type
     *
     * @return
     */
    DataWidgetTypeEnum type();

    void validateBuildCharacteristicsAccordingToDataFacet(
            JSONObject buildCharacteristics,
            Long dataFacetUid) throws AbcUndefinedException;

    JSONObject adjustBuildCharacteristicsAccordingToDataFacet(
            JSONObject buildCharacteristics,
            Long dataFacetUid) throws AbcUndefinedException;
}

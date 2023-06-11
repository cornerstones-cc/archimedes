package cc.cornerstones.biz.datawidget.service.assembly.pivottable;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.biz.datawidget.service.assembly.DataWidgetHandler;
import cc.cornerstones.biz.datawidget.share.constants.DataWidgetTypeEnum;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PivotTableDataWidgetHandler implements DataWidgetHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PivotTableDataWidgetHandler.class);

    /**
     * Data widget type
     *
     * @return
     */
    @Override
    public DataWidgetTypeEnum type() {
        return DataWidgetTypeEnum.PIVOT_TABLE;
    }

    @Override
    public void validateBuildCharacteristicsAccordingToDataFacet(
            JSONObject buildCharacteristics,
            Long dataFacetUid) throws AbcUndefinedException {

    }

    @Override
    public JSONObject adjustBuildCharacteristicsAccordingToDataFacet(
            JSONObject buildCharacteristics,
            Long dataFacetUid) throws AbcUndefinedException {
        return null;
    }
}

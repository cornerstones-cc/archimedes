package cc.cornerstones.biz.serve.service.assembly.datawidget.chart;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.datawidget.share.constants.DataWidgetTypeEnum;
import cc.cornerstones.biz.serve.service.assembly.datawidget.ExecuteDataWidgetHandler;
import cc.cornerstones.biz.share.types.QueryContentResult;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class ExecuteChartDataWidgetHandler implements ExecuteDataWidgetHandler {
    /**
     * Data widget type
     *
     * @return
     */
    @Override
    public DataWidgetTypeEnum type() {
        return DataWidgetTypeEnum.CHART;
    }

    /**
     * Query content of the specified Data Facet
     *
     * @param dataFacetUid
     * @param dataWidgetCharacteristics
     * @param request
     * @param operatingUserProfile
     * @return
     * @throws AbcUndefinedException
     */
    @Override
    public QueryContentResult queryContent(
            Long dataFacetUid,
            JSONObject dataWidgetCharacteristics,
            JSONObject request,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return null;
    }

    /**
     * Export content of the specified Data Facet
     *
     * @param dataFacetUid
     * @param dataWidgetCharacteristics
     * @param request
     * @param operatingUserProfile
     * @return
     * @throws AbcUndefinedException
     */
    @Override
    public Long exportContent(
            Long dataFacetUid,
            JSONObject dataWidgetCharacteristics,
            JSONObject request,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return null;
    }

    @Override
    public Object generateServeCharacteristics(
            Long dataFacetUid) throws AbcUndefinedException {
        return null;
    }
}

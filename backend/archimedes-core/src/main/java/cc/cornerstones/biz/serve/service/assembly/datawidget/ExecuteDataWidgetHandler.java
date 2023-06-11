package cc.cornerstones.biz.serve.service.assembly.datawidget;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.datawidget.share.constants.DataWidgetTypeEnum;
import cc.cornerstones.biz.share.types.QueryContentResult;
import com.alibaba.fastjson.JSONObject;

public interface ExecuteDataWidgetHandler {
    /**
     * Data widget type
     *
     * @return
     */
    DataWidgetTypeEnum type();

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
    QueryContentResult queryContent(
            Long dataFacetUid,
            JSONObject dataWidgetCharacteristics,
            JSONObject request,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

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
    Long exportContent(
            Long dataFacetUid,
            JSONObject dataWidgetCharacteristics,
            JSONObject request,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    /**
     * Generate serve characteristics for the specific Data Facet
     *
     * @param dataFacetUid
     * @return
     * @throws AbcUndefinedException
     */
    Object generateServeCharacteristics(
            Long dataFacetUid) throws AbcUndefinedException;
}

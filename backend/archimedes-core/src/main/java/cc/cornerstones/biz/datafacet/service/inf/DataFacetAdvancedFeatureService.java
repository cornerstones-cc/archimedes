package cc.cornerstones.biz.datafacet.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.datafacet.dto.AdvancedFeatureDto;

public interface DataFacetAdvancedFeatureService {

    AdvancedFeatureDto getAdvancedFeatureOfDataFacet(
            Long dataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void replaceAdvancedFeatureOfDataFacet(
            Long dataFacetUid,
            AdvancedFeatureDto advancedFeatureDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}

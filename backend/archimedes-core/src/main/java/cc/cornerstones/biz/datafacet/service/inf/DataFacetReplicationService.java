package cc.cornerstones.biz.datafacet.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;

import java.util.List;

public interface DataFacetReplicationService {

    void copyDataFacet(
            Long sourceDataFacetUid,
            Long targetDataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}

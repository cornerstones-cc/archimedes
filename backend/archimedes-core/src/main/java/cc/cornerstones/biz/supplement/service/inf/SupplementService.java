package cc.cornerstones.biz.supplement.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import com.alibaba.fastjson.JSONObject;

public interface SupplementService {
    void enableSupplement(
            String name,
            JSONObject configuration,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void disableSupplement(
            String name,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}

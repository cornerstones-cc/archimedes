package cc.cornerstones.biz.supplement.share.types;

import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.supplement.entity.SupplementDo;
import com.alibaba.fastjson.JSONObject;

public interface Supplement {
    String name();

    void validate(JSONObject configuration) throws Exception;

    void onEnabled(SupplementDo supplementDo, UserProfile operatingUserProfile) throws Exception;

    void onDisabled(SupplementDo supplementDo, UserProfile operatingUserProfile) throws Exception;
}

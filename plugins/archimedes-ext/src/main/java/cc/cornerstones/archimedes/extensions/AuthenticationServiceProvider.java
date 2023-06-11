package cc.cornerstones.archimedes.extensions;

import cc.cornerstones.archimedes.extensions.types.SignedInfo;
import com.alibaba.fastjson.JSONObject;
import org.pf4j.ExtensionPoint;

public abstract class AuthenticationServiceProvider implements ExtensionPoint {
    public String getConfigurationTemplate() throws Exception {
        return null;
    }

    public SignedInfo signIn(JSONObject input, String configuration) throws Exception {
        return null;
    }

    public void signOut(JSONObject input, String configuration) throws Exception {

    }

    public JSONObject getUserInfoSchema() throws Exception {
        return null;
    }

}

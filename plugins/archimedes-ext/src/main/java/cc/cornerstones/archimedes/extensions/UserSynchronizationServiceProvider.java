package cc.cornerstones.archimedes.extensions;

import cc.cornerstones.archimedes.extensions.types.UserInfo;
import org.pf4j.ExtensionPoint;

import java.util.List;

public abstract class UserSynchronizationServiceProvider implements ExtensionPoint {
    public String getConfigurationTemplate() throws Exception {
        return null;
    }

    public List<UserInfo> listingQueryAllUsers(String configuration) throws Exception {
        return null;
    }
}

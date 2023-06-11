package cc.cornerstones.biz.share.event;

import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.serviceconnection.entity.DataPermissionServiceAgentDo;
import cc.cornerstones.biz.administration.serviceconnection.entity.DataPermissionServiceComponentDo;
import lombok.Data;

@Data
public class DataPermissionServiceAgentDeletedEvent {
    private DataPermissionServiceAgentDo dataPermissionServiceAgentDo;
    private UserProfile operatingUserProfile;
}

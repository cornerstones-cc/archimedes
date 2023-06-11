package cc.cornerstones.biz.share.event;

import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.serviceconnection.entity.DataPermissionServiceComponentDo;
import cc.cornerstones.biz.administration.serviceconnection.entity.DfsServiceComponentDo;
import lombok.Data;

@Data
public class DataPermissionServiceComponentDeletedEvent {
    private DataPermissionServiceComponentDo dataPermissionServiceComponentDo;
    private UserProfile operatingUserProfile;
}

package cc.cornerstones.biz.share.event;

import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.serviceconnection.entity.DfsServiceComponentDo;
import cc.cornerstones.biz.administration.serviceconnection.entity.UserSynchronizationServiceComponentDo;
import lombok.Data;

@Data
public class UserSynchronizationServiceComponentDeletedEvent {
    private UserSynchronizationServiceComponentDo userSynchronizationServiceComponentDo;
    private UserProfile operatingUserProfile;
}

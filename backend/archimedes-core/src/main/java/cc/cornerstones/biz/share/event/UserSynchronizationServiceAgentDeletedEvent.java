package cc.cornerstones.biz.share.event;

import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.serviceconnection.entity.UserSynchronizationServiceAgentDo;
import cc.cornerstones.biz.administration.serviceconnection.entity.UserSynchronizationServiceComponentDo;
import lombok.Data;

@Data
public class UserSynchronizationServiceAgentDeletedEvent {
    private UserSynchronizationServiceAgentDo userSynchronizationServiceAgentDo;
    private UserProfile operatingUserProfile;
}

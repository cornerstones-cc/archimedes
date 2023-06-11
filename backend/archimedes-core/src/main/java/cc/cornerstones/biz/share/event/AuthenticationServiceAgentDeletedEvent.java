package cc.cornerstones.biz.share.event;

import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.serviceconnection.entity.AuthenticationServiceAgentDo;
import cc.cornerstones.biz.administration.serviceconnection.entity.AuthenticationServiceComponentDo;
import lombok.Data;

@Data
public class AuthenticationServiceAgentDeletedEvent {
    private AuthenticationServiceAgentDo authenticationServiceAgentDo;
    private UserProfile operatingUserProfile;
}

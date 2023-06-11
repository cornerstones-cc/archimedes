package cc.cornerstones.biz.share.event;

import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.serviceconnection.entity.AuthenticationServiceComponentDo;
import lombok.Data;

@Data
public class AuthenticationServiceComponentDeletedEvent {
    private AuthenticationServiceComponentDo authenticationServiceComponentDo;
    private UserProfile operatingUserProfile;
}

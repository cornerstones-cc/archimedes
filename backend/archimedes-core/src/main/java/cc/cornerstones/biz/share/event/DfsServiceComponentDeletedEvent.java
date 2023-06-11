package cc.cornerstones.biz.share.event;

import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.serviceconnection.entity.AuthenticationServiceComponentDo;
import cc.cornerstones.biz.administration.serviceconnection.entity.DfsServiceComponentDo;
import lombok.Data;

@Data
public class DfsServiceComponentDeletedEvent {
    private DfsServiceComponentDo dfsServiceComponentDo;
    private UserProfile operatingUserProfile;
}

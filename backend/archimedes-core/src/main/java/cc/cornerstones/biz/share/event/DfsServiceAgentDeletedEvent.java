package cc.cornerstones.biz.share.event;

import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.serviceconnection.entity.DfsServiceAgentDo;
import lombok.Data;

@Data
public class DfsServiceAgentDeletedEvent {
    private DfsServiceAgentDo dfsServiceAgentDo;
    private UserProfile operatingUserProfile;
}

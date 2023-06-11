package cc.cornerstones.biz.share.event;

import cc.cornerstones.almond.types.UserProfile;
import lombok.Data;

@Data
public class GroupDeletedEvent {
    private Long uid;
    private UserProfile operatingUserProfile;
}

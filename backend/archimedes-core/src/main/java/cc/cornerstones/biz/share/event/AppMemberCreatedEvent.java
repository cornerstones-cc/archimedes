package cc.cornerstones.biz.share.event;

import cc.cornerstones.almond.types.UserProfile;
import lombok.Data;

@Data
public class AppMemberCreatedEvent {
    private Long appUid;
    private Long userUid;
    private UserProfile operatingUserProfile;
}

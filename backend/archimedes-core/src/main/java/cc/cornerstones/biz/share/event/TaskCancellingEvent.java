package cc.cornerstones.biz.share.event;

import cc.cornerstones.almond.types.UserProfile;
import lombok.Data;

@Data
public class TaskCancellingEvent {
    private Long taskUid;
    private UserProfile operatingUserProfile;
}

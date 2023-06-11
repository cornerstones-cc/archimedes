package cc.cornerstones.biz.share.event;

import cc.cornerstones.almond.types.UserProfile;
import lombok.Data;

import java.util.List;

@Data
public class UserGroupChangedEvent {
    private Long userUid;
    private List<Long> newGroupUidList;
    private UserProfile operatingUserProfile;
}

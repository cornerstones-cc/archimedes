package cc.cornerstones.biz.share.event;

import cc.cornerstones.almond.types.UserProfile;
import lombok.Data;

import java.util.List;

@Data
public class UserRoleChangedEvent {
    private Long userUid;
    private List<Long> newRoleUidList;
    private UserProfile operatingUserProfile;
}

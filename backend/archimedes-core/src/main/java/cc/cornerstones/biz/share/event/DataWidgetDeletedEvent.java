package cc.cornerstones.biz.share.event;

import cc.cornerstones.almond.types.UserProfile;
import lombok.Data;

@Data
public class DataWidgetDeletedEvent {
    private Long uid;
    private UserProfile operatingUserProfile;
}

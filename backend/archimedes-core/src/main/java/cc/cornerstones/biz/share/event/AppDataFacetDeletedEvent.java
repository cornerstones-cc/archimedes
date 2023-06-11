package cc.cornerstones.biz.share.event;

import cc.cornerstones.almond.types.UserProfile;
import lombok.Data;

import java.util.List;

@Data
public class AppDataFacetDeletedEvent {
    private List<Long> uidList;
    private UserProfile operatingUserProfile;
}

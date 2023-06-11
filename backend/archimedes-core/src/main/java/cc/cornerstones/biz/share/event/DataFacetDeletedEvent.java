package cc.cornerstones.biz.share.event;

import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.datafacet.entity.DataFacetDo;
import lombok.Data;

@Data
public class DataFacetDeletedEvent {
    private DataFacetDo dataFacetDo;
    private UserProfile operatingUserProfile;
}

package cc.cornerstones.biz.share.event;

import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.datasource.entity.DataSourceDo;
import lombok.Data;

@Data
public class DataSourceDeletedEvent {
    private DataSourceDo dataSourceDo;
    private UserProfile operatingUserProfile;
}

package cc.cornerstones.biz.share.event;

import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.datatable.entity.DataTableDo;
import lombok.Data;

@Data
public class DirectDataTableDeletedEvent {
    private DataTableDo dataTableDo;
    private UserProfile operatingUserProfile;
}

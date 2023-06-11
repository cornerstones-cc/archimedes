package cc.cornerstones.biz.share.event;

import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.datasource.service.assembly.database.DataColumnMetadata;
import cc.cornerstones.biz.datatable.entity.DataColumnDo;
import cc.cornerstones.biz.datatable.entity.DataTableDo;
import lombok.Data;

import java.util.List;

@Data
public class DirectDataTableStructureChangedEvent {
    private DataTableDo dataTableDo;
    private List<DataColumnDo> dataColumnDoList;
    private UserProfile operatingUserProfile;
}

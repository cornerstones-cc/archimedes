package cc.cornerstones.biz.share.event;

import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.datasource.entity.DataSourceDo;
import cc.cornerstones.biz.datasource.service.assembly.database.DataColumnMetadata;
import cc.cornerstones.biz.datasource.service.assembly.database.DataIndexMetadata;
import cc.cornerstones.biz.datasource.service.assembly.database.DataTableMetadata;
import lombok.Data;

import java.util.List;

@Data
public class DataSourceMetadataRefreshedEvent {
    private DataSourceDo dataSourceDo;
    private List<DataTable> dataTableList;
    private UserProfile operatingUserProfile;

    @Data
    public static class DataTable {
        private DataTableMetadata dataTableMetadata;
        private List<DataColumnMetadata> dataColumnMetadataList;
        private List<DataIndexMetadata> dataIndexMetadataList;
    }
}

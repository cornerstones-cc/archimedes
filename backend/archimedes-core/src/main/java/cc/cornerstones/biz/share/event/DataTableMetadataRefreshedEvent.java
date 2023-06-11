package cc.cornerstones.biz.share.event;

import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.datasource.entity.DataSourceDo;
import cc.cornerstones.biz.datasource.service.assembly.database.DataColumnMetadata;
import cc.cornerstones.biz.datasource.service.assembly.database.DataIndexMetadata;
import cc.cornerstones.biz.datasource.service.assembly.database.DataTableMetadata;
import cc.cornerstones.biz.datatable.entity.DataTableDo;
import lombok.Data;

import java.util.List;

@Data
public class DataTableMetadataRefreshedEvent {
    /**
     * data source
     */
    private DataSourceDo dataSourceDo;

    /**
     * optional
     */
    private DataTableDo dataTableDo;

    private DataTableMetadata dataTableMetadata;
    private List<DataColumnMetadata> dataColumnMetadataList;
    private List<DataIndexMetadata> dataIndexMetadataList;
    private UserProfile operatingUserProfile;
}

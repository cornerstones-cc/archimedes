package cc.cornerstones.biz.datasource.service.assembly.database.mssql;

import lombok.Data;

@Data
public class MssqlDataTableMetadataExtension {
    private String databaseName;
    private String schemaName;
    private String tableName;
    private String tableComment;
    private String tableType;
}

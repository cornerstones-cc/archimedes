package cc.cornerstones.biz.datasource.service.assembly.database.postgresql;

import lombok.Data;

@Data
public class PostgresqlDataTableMetadataExtension {
    private String databaseName;
    private String tableName;
    private String tableComment;
    private String tableType;
}

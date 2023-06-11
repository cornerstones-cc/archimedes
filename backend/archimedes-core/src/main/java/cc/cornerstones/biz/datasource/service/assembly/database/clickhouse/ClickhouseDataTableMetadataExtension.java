package cc.cornerstones.biz.datasource.service.assembly.database.clickhouse;

import lombok.Data;

@Data
public class ClickhouseDataTableMetadataExtension {
    private String databaseName;
    private String tableName;
    private String tableComment;
    private String tableType;
}

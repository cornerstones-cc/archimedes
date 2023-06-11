package cc.cornerstones.biz.datasource.service.assembly.database.mysql;

import lombok.Data;

@Data
public class MysqlDataTableMetadataExtension {
    private String databaseName;
    private String tableName;
    private String tableComment;
    private String tableType;
}

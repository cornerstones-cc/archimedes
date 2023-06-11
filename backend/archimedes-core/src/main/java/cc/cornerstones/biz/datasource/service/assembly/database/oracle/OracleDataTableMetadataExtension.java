package cc.cornerstones.biz.datasource.service.assembly.database.oracle;

import lombok.Data;

@Data
public class OracleDataTableMetadataExtension {
    private String databaseName;
    private String tableName;
    private String tableComment;
    private String tableType;
}

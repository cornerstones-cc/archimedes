package cc.cornerstones.biz.datasource.service.assembly.database.oracle;

import cc.cornerstones.biz.datasource.service.assembly.database.DataSourceConnection;
import lombok.Data;

import java.sql.Connection;

@Data
public class OracleConnection extends DataSourceConnection {
    private Connection connection;
}

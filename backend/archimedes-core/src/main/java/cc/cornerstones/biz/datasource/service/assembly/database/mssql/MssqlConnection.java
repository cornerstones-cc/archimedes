package cc.cornerstones.biz.datasource.service.assembly.database.mssql;

import cc.cornerstones.biz.datasource.service.assembly.database.DataSourceConnection;
import lombok.Data;

import java.sql.Connection;

@Data
public class MssqlConnection extends DataSourceConnection {
    private Connection connection;
}

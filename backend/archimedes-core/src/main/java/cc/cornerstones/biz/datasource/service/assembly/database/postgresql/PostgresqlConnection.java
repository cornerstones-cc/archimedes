package cc.cornerstones.biz.datasource.service.assembly.database.postgresql;

import cc.cornerstones.biz.datasource.service.assembly.database.DataSourceConnection;
import lombok.Data;

import java.sql.Connection;

@Data
public class PostgresqlConnection extends DataSourceConnection {
    private Connection connection;
}

package cc.cornerstones.biz.datasource.service.assembly.database.mysql;

import cc.cornerstones.biz.datasource.service.assembly.database.DataSourceConnection;
import lombok.Data;

import java.sql.Connection;

@Data
public class MysqlConnection extends DataSourceConnection {
    private Connection connection;
}

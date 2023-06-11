package cc.cornerstones.biz.datasource.service.assembly.database.clickhouse;

import cc.cornerstones.biz.datasource.service.assembly.database.DataSourceConnection;
import lombok.Data;

import java.sql.Connection;

@Data
public class ClickhouseConnection extends DataSourceConnection {
    private Connection connection;
}

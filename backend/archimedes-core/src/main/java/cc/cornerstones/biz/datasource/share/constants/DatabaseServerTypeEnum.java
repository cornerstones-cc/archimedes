package cc.cornerstones.biz.datasource.share.constants;

/**
 * Database server type
 *
 * @author bbottong
 */
public enum DatabaseServerTypeEnum {
    MSSQL("Microsoft SQL Server"),
    MYSQL("MySQL"),
    POSTGRESQL("PostgreSQL"),
    ORACLE("Oracle"),
    CLICKHOUSE("ClickHouse");

    final String symbol;

    DatabaseServerTypeEnum(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return this.symbol;
    }
}

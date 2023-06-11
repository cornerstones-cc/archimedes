package cc.cornerstones.biz.datasource.service.assembly.database.postgresql;

import cc.cornerstones.almond.types.AbcTuple2;
import lombok.Data;

import java.util.List;

@Data
public class PostgresqlConnectionProfile {
    private String host;
    private Integer port;
    private String user;
    private String password;
    private String database;
    private List<AbcTuple2<String, String>> properties;
}

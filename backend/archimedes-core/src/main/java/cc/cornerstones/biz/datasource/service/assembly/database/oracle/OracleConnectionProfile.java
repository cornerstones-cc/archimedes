package cc.cornerstones.biz.datasource.service.assembly.database.oracle;

import cc.cornerstones.almond.types.AbcTuple2;
import lombok.Data;

import java.util.List;

@Data
public class OracleConnectionProfile {
    private String host;
    private Integer port;
    private String user;
    private String password;
    private UrlPattern urlPattern;

    /**
     * if urlPattern = SID, the database is SID;
     * else if urlPattern = SERVICE_NAME, the database is service name
     */
    private String endpoint;

    private String internalLogon;

    private List<AbcTuple2<String, String>> properties;

    public enum UrlPattern {
        SID,
        SERVICE_NAME;
    }
}

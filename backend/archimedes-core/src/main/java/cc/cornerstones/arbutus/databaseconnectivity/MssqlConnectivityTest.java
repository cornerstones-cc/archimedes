package cc.cornerstones.arbutus.databaseconnectivity;

import cc.cornerstones.almond.types.AbcTuple2;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class MssqlConnectivityTest {
    @NotNull(message = "host is required")
    private String host;

    @NotNull(message = "port is required")
    private Integer port;

    @NotNull(message = "user is required")
    private String user;

    @NotNull(message = "password is required")
    private String password;

    private String database;

    private List<AbcTuple2<String, String>> properties;
}

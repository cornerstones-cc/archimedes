package cc.cornerstones.almond.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * User's Profile
 *
 * @author bbottong
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class UserProfile {
    private Long uid;

    private String displayName;

    private LocalDateTime createdTimestamp;

    private LocalDateTime lastModifiedTimestamp;

    /**
     * tracking serial number
     */
    private String trackingSerialNumber;

    /**
     * true --- open api access, false --- user access
     */
    private Boolean openApi;

    /**
     * if openApi is true, should fill in appUid
     */
    private Long appUid;

    /**
     * if openApi is true, should fill in appUserUid
     */
    private Long appUserUid;
}

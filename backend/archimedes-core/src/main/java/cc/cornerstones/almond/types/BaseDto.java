package cc.cornerstones.almond.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * @author bbottong
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public abstract class BaseDto {
    private LocalDateTime createdTimestamp;
    private Long createdBy;
    private LocalDateTime lastModifiedTimestamp;
    private Long lastModifiedBy;
    private Long owner;

    private UserBriefInformation createdByUser;
    private UserBriefInformation lastModifiedByUser;
    private UserBriefInformation ownerUser;
}
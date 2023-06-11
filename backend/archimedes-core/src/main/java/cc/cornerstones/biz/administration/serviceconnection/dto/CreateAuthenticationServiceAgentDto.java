package cc.cornerstones.biz.administration.serviceconnection.dto;

import cc.cornerstones.almond.types.AbcTuple2;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class CreateAuthenticationServiceAgentDto {

    /**
     * Name
     *
     * A name is used to identify the object.
     */
    @NotBlank(message = "name is required")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5_a-zA-Z0-9\\s-]+$", message = "Only Chinese characters, or English letters, or numbers, or spaces, or underscores, or hyphens are allowed")
    @Size(min = 1, max = 64,
            message = "name cannot exceed 64 characters in length")
    private String name;

    /**
     * Description
     *
     * A meaning description helps you remembers the differences between objects.
     */
    @Size(min = 0, max = 255,
            message = "description cannot exceed 255 characters in length")
    private String description;

    /**
     * Sequence
     */
    private Float sequence;

    /**
     * Enabled
     */
    private Boolean enabled;

    /**
     * Preferred
     */
    private Boolean preferred;

    /**
     * Service component uid
     */
    @NotNull(message = "service_component_uid is required")
    private Long serviceComponentUid;

    /**
     * Configuration
     */
    private String configuration;

    /**
     * Account type's uid
     */
    private List<Long> accountTypeUidList;

    /**
     * User schema extended properties mapping to userinfo
     * f (field 1) --- extended property's UID,
     * s (field 2) --- field path (eg., a.b.c) in the userinfo result object
     */
    private List<AbcTuple2<Long, String>> properties;
}

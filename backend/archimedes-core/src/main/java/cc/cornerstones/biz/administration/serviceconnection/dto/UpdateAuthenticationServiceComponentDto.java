package cc.cornerstones.biz.administration.serviceconnection.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class UpdateAuthenticationServiceComponentDto {

    /**
     * Name
     *
     * A name is used to identify the object.
     */
    @NotBlank(message = "name is required")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5_a-zA-Z0-9\\s-]+$", message = "Only Chinese characters, or English letters, or numbers, or spaces, or underscores, or hyphens are allowed")
    @Size(min = 1, max = 64,
            message = "The name cannot exceed 64 characters in length")
    private String name;

    /**
     * Description
     *
     * A meaning description helps you remembers the differences between objects.
     */
    @Size(min = 0, max = 255,
            message = "The description cannot exceed 255 characters in length")
    private String description;

    /**
     * Sequence
     */
    private Float sequence;

    /**
     * Front-end component file id
     */
    private String frontEndComponentFileId;

    /**
     * DFS service agent uid of front-end component file id
     */
    private Long dfsServiceAgentUidOfFrontEndComponentFileId;

    /**
     * Back-end component file id
     */
    private String backEndComponentFileId;

    /**
     * DFS service agent uid of back-end component file id
     */
    private Long dfsServiceAgentUidOfBackEndComponentFileId;
}

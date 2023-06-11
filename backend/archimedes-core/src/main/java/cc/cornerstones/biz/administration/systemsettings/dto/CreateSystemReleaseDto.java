package cc.cornerstones.biz.administration.systemsettings.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class CreateSystemReleaseDto {
    @NotBlank(message = "system_name is required")
    @Size(min = 0, max = 100,
            message = "system_name length exceeds limit")
    private String systemName;

    @Size(min = 0, max = 200,
            message = "system_logo_url length exceeds limit")
    private String systemLogoUrl;

    @Size(min = 0, max = 200,
            message = "big_picture_url length exceeds limit")
    private String bigPictureUrl;

    @Size(min = 0, max = 200,
            message = "terms_of_service_url length exceeds limit")
    private String termsOfServiceUrl;

    @Size(min = 0, max = 200,
            message = "privacy_policy_url length exceeds limit")
    private String privacyPolicyUrl;

    @NotBlank(message = "release_version is required")
    @Size(min = 0, max = 45,
            message = "release_version length exceeds limit")
    private String releaseVersion;

    @Size(min = 0, max = 100,
            message = "vendor_name length exceeds limit")
    private String vendorName;
}

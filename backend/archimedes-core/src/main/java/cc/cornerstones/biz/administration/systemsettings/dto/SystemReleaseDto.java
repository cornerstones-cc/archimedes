package cc.cornerstones.biz.administration.systemsettings.dto;

import cc.cornerstones.almond.types.BaseDto;
import lombok.Data;

@Data
public class SystemReleaseDto extends BaseDto {
    private Long uid;
    private String systemName;
    private String systemLogoUrl;
    private String bigPictureUrl;
    private String termsOfServiceUrl;
    private String privacyPolicyUrl;
    private String releaseVersion;
    private String vendorName;
}

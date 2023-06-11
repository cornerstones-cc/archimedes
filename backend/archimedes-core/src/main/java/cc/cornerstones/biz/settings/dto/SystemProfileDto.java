package cc.cornerstones.biz.settings.dto;

import cc.cornerstones.almond.types.BaseDto;
import lombok.Data;

@Data
public class SystemProfileDto extends BaseDto {
    private String systemName;
    private String systemLogoUrl;
    private String bigPictureUrl;
    private String termsOfServiceUrl;
    private String privacyPolicyUrl;
    private String releaseVersion;
    private String vendorName;
}

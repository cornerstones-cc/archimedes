package cc.cornerstones.biz.app.dto;

import cc.cornerstones.almond.types.BaseDto;
import lombok.Data;

@Data
public class AppOpenApiCredentialDto extends BaseDto {
    /**
     * App's uid
     */
    private Long appUid;

    /**
     * App's key
     */
    private String appKey;

    /**
     * App's secret
     */
    private String appSecret;

}

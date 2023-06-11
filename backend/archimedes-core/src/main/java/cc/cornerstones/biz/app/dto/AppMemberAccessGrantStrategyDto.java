package cc.cornerstones.biz.app.dto;

import cc.cornerstones.almond.types.BaseDto;
import lombok.Data;


@Data
public class AppMemberAccessGrantStrategyDto extends BaseDto {

    /**
     * entire grant
     */
    private Boolean enabledEntireGrant;

    /**
     * granular grant
     */
    private Boolean enabledGranularGrant;
}

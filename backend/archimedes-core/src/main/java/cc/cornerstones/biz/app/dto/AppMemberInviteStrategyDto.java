package cc.cornerstones.biz.app.dto;

import cc.cornerstones.almond.types.BaseDto;
import cc.cornerstones.biz.app.share.constants.AppMembershipEnum;
import lombok.Data;

import java.util.List;

@Data
public class AppMemberInviteStrategyDto extends BaseDto {
    /**
     * automatically invite users with the specified roles
     */
    private Boolean enabledRoles;

    private List<Long> roleUidList;

    /**
     * automatically invite users who belong to the specified groups
     */
    private Boolean enabledGroups;

    private List<Long> groupUidList;

    private AppMembershipEnum membership;
}

package cc.cornerstones.biz.app.dto;

import cc.cornerstones.almond.types.BaseDto;
import cc.cornerstones.biz.administration.usermanagement.share.types.Account;
import cc.cornerstones.biz.administration.usermanagement.share.types.ExtendedProperty;
import cc.cornerstones.biz.administration.usermanagement.share.types.Group;
import cc.cornerstones.biz.administration.usermanagement.share.types.Role;
import cc.cornerstones.biz.app.share.constants.AppMembershipEnum;
import lombok.Data;

import java.util.List;

@Data
public class AppMemberDto extends BaseDto {
    /**
     * App's uid
     */
    private Long appUid;

    /**
     * App membership
     */
    private AppMembershipEnum membership;

    /**
     * App member's user uid
     */
    private Long userUid;
}

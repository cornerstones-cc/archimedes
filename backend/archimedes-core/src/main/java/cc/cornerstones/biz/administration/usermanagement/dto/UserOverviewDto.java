package cc.cornerstones.biz.administration.usermanagement.dto;

import cc.cornerstones.almond.types.BaseDto;
import cc.cornerstones.almond.types.UserBriefInformation;
import cc.cornerstones.biz.administration.usermanagement.share.constants.UserTypeEnum;
import cc.cornerstones.biz.administration.usermanagement.share.types.Account;
import cc.cornerstones.biz.administration.usermanagement.share.types.ExtendedProperty;
import cc.cornerstones.biz.administration.usermanagement.share.types.Group;
import cc.cornerstones.biz.administration.usermanagement.share.types.Role;
import lombok.Data;

import java.util.List;

@Data
public class UserOverviewDto extends BaseDto {
    private List<Role> roleList;

    private List<Group> groupList;

    private UserBriefInformation user;
}

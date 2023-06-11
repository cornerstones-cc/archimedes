package cc.cornerstones.biz.administration.usermanagement.dto;

import cc.cornerstones.almond.types.BaseDto;
import cc.cornerstones.almond.types.UserBriefInformation;
import cc.cornerstones.biz.administration.usermanagement.share.constants.UserTypeEnum;
import cc.cornerstones.biz.administration.usermanagement.share.types.*;
import lombok.Data;

import java.util.List;

@Data
public class UserDto extends BaseDto {
    private Long uid;

    private String displayName;

    private Boolean enabled;

    private UserTypeEnum type;

    private List<ExtendedProperty> extendedPropertyList;

    private List<Account> accountList;

    private List<Role> roleList;

    private List<Group> groupList;
}

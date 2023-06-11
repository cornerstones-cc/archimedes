package cc.cornerstones.biz.administration.usermanagement.dto;

import cc.cornerstones.biz.administration.usermanagement.share.types.Account;
import cc.cornerstones.biz.administration.usermanagement.share.types.ExtendedProperty;
import cc.cornerstones.biz.administration.usermanagement.share.types.Group;
import cc.cornerstones.biz.administration.usermanagement.share.types.Role;
import lombok.Data;

import java.util.List;

@Data
public class UserOutlineDto {
    private Long uid;

    private String displayName;

    private List<ExtendedProperty> extendedPropertyList;

    private List<Account> accountList;

    private List<Role> roleList;

    private List<Group> groupList;
}

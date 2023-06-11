package cc.cornerstones.biz.administration.usermanagement.dto;

import cc.cornerstones.almond.types.BaseDto;
import cc.cornerstones.biz.administration.usermanagement.share.types.Account;
import cc.cornerstones.biz.administration.usermanagement.share.types.ExtendedProperty;
import cc.cornerstones.biz.administration.usermanagement.share.types.Group;
import cc.cornerstones.biz.administration.usermanagement.share.types.Role;
import lombok.Data;

import java.util.List;

@Data
public class UserSimplifiedDto extends BaseDto {
    private Long uid;

    private String displayName;

    private Boolean enabled;

    private List<ExtendedProperty> extendedPropertyList;

    private List<Account> accountList;
}

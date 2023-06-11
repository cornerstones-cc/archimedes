package cc.cornerstones.biz.app.dto;

import cc.cornerstones.almond.types.BaseDto;
import cc.cornerstones.biz.administration.usermanagement.share.types.Account;
import cc.cornerstones.biz.administration.usermanagement.share.types.ExtendedProperty;
import cc.cornerstones.biz.administration.usermanagement.share.types.Group;
import cc.cornerstones.biz.administration.usermanagement.share.types.Role;
import cc.cornerstones.biz.datafacet.dto.SimplifiedDataFacetDto;
import lombok.Data;

import java.util.List;

@Data
public class AppMemberAccessGrantDto extends BaseDto {
    /**
     * App's uid
     */
    private Long appUid;

    /**
     * Granted data facet(s)
     */
    private List<SimplifiedDataFacetDto> dataFacetDtoList;

    /**
     * User's uid
     */
    private Long uid;

    /**
     * User's display name
     */
    private String displayName;

    /**
     * User's extended properties
     */
    private List<ExtendedProperty> extendedPropertyList;

    /**
     * User's accounts
     */
    private List<Account> accountList;

    /**
     * User's roles
     */
    private List<Role> roleList;

    /**
     * User's groups
     */
    private List<Group> groupList;

}

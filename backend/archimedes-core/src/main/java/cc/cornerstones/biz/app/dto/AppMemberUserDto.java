package cc.cornerstones.biz.app.dto;

import cc.cornerstones.almond.types.UserBriefInformation;
import cc.cornerstones.biz.administration.usermanagement.share.types.Account;
import cc.cornerstones.biz.administration.usermanagement.share.types.ExtendedProperty;
import cc.cornerstones.biz.administration.usermanagement.share.types.Group;
import cc.cornerstones.biz.administration.usermanagement.share.types.Role;
import cc.cornerstones.biz.app.share.constants.AppMembershipEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class AppMemberUserDto {
    /**
     * App member's created timestamp
     */
    private LocalDateTime createdTimestamp;
    /**
     * App member's created by
     */
    private Long createdBy;
    /**
     * App member's last modified timestamp
     */
    private LocalDateTime lastModifiedTimestamp;
    /**
     * App member's last modified by
     */
    private Long lastModifiedBy;
    /**
     * App member's owner
     */
    private Long owner;

    /**
     * App's uid
     */
    private Long appUid;

    /**
     * App member's membership
     */
    private AppMembershipEnum membership;

    /**
     *  App member's user roles
     */
    private List<Role> roleList;

    /**
     *  App member's user groups
     */
    private List<Group> groupList;

    private UserBriefInformation createdByUser;
    private UserBriefInformation lastModifiedByUser;
    private UserBriefInformation user;
}

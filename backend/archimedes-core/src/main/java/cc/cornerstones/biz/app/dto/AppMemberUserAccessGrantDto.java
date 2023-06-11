package cc.cornerstones.biz.app.dto;

import cc.cornerstones.almond.types.UserBriefInformation;
import cc.cornerstones.biz.administration.usermanagement.share.types.Account;
import cc.cornerstones.biz.administration.usermanagement.share.types.ExtendedProperty;
import cc.cornerstones.biz.administration.usermanagement.share.types.Group;
import cc.cornerstones.biz.administration.usermanagement.share.types.Role;
import cc.cornerstones.biz.app.share.constants.AppMembershipEnum;
import cc.cornerstones.biz.datafacet.dto.SimplifiedDataFacetDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class AppMemberUserAccessGrantDto {
    /**
     * App's uid
     */
    private Long appUid;

    /**
     * App's data facet hierarchy node uid
     *
     * 2022/11/30 为了兼容前端，名字改成 dataFacetHierarchyNodeUid
     */
    private List<Long> dataFacetHierarchyNodeUidList;

    /**
     *  App member's user roles
     */
    private List<Role> roleList;

    /**
     *  App member's user groups
     */
    private List<Group> groupList;

    private UserBriefInformation user;
}

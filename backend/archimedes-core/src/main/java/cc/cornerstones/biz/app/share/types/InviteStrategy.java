package cc.cornerstones.biz.app.share.types;

import cc.cornerstones.biz.app.share.constants.AppMembershipEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;


@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class InviteStrategy {

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

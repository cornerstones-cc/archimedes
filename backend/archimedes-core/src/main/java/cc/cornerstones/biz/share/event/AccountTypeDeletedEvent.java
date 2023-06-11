package cc.cornerstones.biz.share.event;

import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.usermanagement.entity.AccountTypeDo;
import lombok.Data;

@Data
public class AccountTypeDeletedEvent {
    private AccountTypeDo accountTypeDo;
    private UserProfile operatingUserProfile;
}

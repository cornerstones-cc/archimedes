package cc.cornerstones.biz.administration.usermanagement.dto;

import cc.cornerstones.biz.administration.usermanagement.share.types.*;
import lombok.Data;

@Data
public class UserDetailedDto extends UserDto {
    private Permissions permissions;
}

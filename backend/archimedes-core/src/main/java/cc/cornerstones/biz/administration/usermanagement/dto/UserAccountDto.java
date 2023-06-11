package cc.cornerstones.biz.administration.usermanagement.dto;

import cc.cornerstones.almond.types.BaseDto;
import lombok.Data;

@Data
public class UserAccountDto extends BaseDto {
    /**
     * Name
     */
    private String name;

    /**
     * 所属 Account type 的 uid
     */
    private Long accountTypeUid;

    /**
     * 所属 User 的 UID
     */
    private Long userUid;
}
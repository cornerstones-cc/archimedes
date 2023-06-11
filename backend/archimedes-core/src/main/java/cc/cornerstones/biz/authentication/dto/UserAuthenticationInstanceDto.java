package cc.cornerstones.biz.authentication.dto;

import cc.cornerstones.almond.types.BaseDto;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserAuthenticationInstanceDto extends BaseDto {

    /**
     * UID
     */
    private Long uid;

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 令牌类型
     */
    private String tokenType;

    /**
     * 令牌失效前剩余的秒数
     */
    private Integer expiresInSeconds;

    /**
     * 令牌失效时间戳
     */
    private LocalDateTime expiresAtTimestamp;

    /**
     * 令牌是否已经失效，true - 已失效，false - 没有失效
     */
    private Boolean expired;

    /**
     * 刷新令牌
     */
    private String refreshToken;

    /**
     * 所属 User 的 uid
     */
    private Long userUid;

    /**
     * 所属 User 的 display name
     */
    private String userDisplayName;

    /**
     * 所属 User 在 Sign in 时所使用的 Account type 的 uid
     */
    private Long userAccountTypeUid;

    /**
     * 所属 User 在 Sign in 时所使用的 Account 的 name
     */
    private String userAccountName;
}

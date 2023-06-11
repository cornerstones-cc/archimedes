package cc.cornerstones.biz.authentication.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class UserSignedInDto {
    /**
     * 用户 UID
     */
    private Long userUid;

    /**
     * 用户 Display name
     */
    private String userDisplayName;

    /**
     * 用户 Sign in 所使用的 Account type uid
     */
    private Long userAccountTypeUid;

    /**
     * 用户 Sign in 所使用的 Account name
     */
    private String userAccountName;

    /**
     * 令牌
     */
    private String accessToken;

    /**
     * 令牌类型
     */
    private String tokenType;

    /**
     * 令牌失效之前剩余的秒数
     */
    private Integer expiresInSeconds;

    /**
     * 令牌失效时间戳
     */
    private LocalDateTime expiresAtTimestamp;

    /**
     * 创建时间戳
     */
    private LocalDateTime createdTimestamp;
}

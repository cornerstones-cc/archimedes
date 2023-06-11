package cc.cornerstones.biz.authentication.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class OpenApiSignedInDto {
    /**
     * 令牌
     */
    private String accessToken;

    /**
     * 刷新令牌的令牌
     */
    private String refreshToken;

    /**
     * 令牌类型
     */
    private String tokenType;

    /**
     * 令牌失效之前剩余的秒数
     */
    private Integer expiresIn;

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

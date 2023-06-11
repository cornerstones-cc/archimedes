package cc.cornerstones.biz.authentication.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author bbottong
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class RefreshedAccessTokenDto {
    private String accessToken;

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
}

package cc.cornerstones.biz.authentication.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Open api 鉴权记录
 *
 */
@TinyId(bizType = OpenApiAuthenticationInstanceDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 10, remainder = 0,
        delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = OpenApiAuthenticationInstanceDo.RESOURCE_NAME,
        indexes = {
                @Index(name = "app_uid_n_is_revoked", columnList = "app_uid, is_revoked", unique = false)
        })
@Where(clause = "is_deleted=0")
public class OpenApiAuthenticationInstanceDo extends BaseDo {
    public static final String RESOURCE_NAME = "k5_open_api_authentication_instance";
    public static final String RESOURCE_SYMBOL = "Open api authentication instance";

    /**
     * UID
     */
    private Long uid;

    /**
     * 访问令牌
     */
    @Column(name = "access_token", length = 255)
    private String accessToken;

    /**
     * 令牌类型
     */
    @Column(name = "token_type", length = 45)
    private String tokenType;

    /**
     * 令牌失效前剩余的秒数
     */
    @Column(name = "expires_in_seconds")
    private Integer expiresInSeconds;

    /**
     * 令牌失效时间戳
     */
    @Column(name = "expires_at_timestamp")
    private LocalDateTime expiresAtTimestamp;

    /**
     * 令牌是否已经失效，true - 已失效，false - 没有失效
     */
    @Column(name = "is_revoked")
    private Boolean revoked;

    /**
     * 刷新令牌
     */
    @Column(name = "refresh_token", length = 255)
    private String refreshToken;

    /**
     * 所属 App 的 uid
     */
    @Column(name = "app_uid")
    private Long appUid;

    /**
     * 所属 App 在 Sign in 时所使用的 app key
     */
    @Column(name = "app_key")
    private String appKey;
}

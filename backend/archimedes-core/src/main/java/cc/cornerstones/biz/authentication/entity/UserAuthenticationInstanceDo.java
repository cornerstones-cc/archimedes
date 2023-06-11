package cc.cornerstones.biz.authentication.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import cc.cornerstones.biz.administration.usermanagement.dto.UserOutlineDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 用户鉴权记录
 *
 */
@TinyId(bizType = UserAuthenticationInstanceDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 10, remainder = 0,
        delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = UserAuthenticationInstanceDo.RESOURCE_NAME,
        indexes = {
                @Index(name = "user_uid_n_is_revoked", columnList = "user_uid, is_revoked", unique = false)
        })
@Where(clause = "is_deleted=0")
public class UserAuthenticationInstanceDo extends BaseDo {
    public static final String RESOURCE_NAME = "k5_user_authentication_instance";
    public static final String RESOURCE_SYMBOL = "User authentication instance";

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
     * 所属 User 的 uid
     */
    @Column(name = "user_uid")
    private Long userUid;

    /**
     * 所属 User 的 display name
     */
    @Column(name = "user_display_name", length = 64)
    private String userDisplayName;

    /**
     * 所属 User 在 Sign in 时所使用的 Account type 的 uid
     */
    @Column(name = "user_account_type_uid")
    private Long userAccountTypeUid;

    /**
     * 所属 User 在 Sign in 时所使用的 Account 的 name
     */
    @Column(name = "user_account_name", length = 64)
    private String userAccountName;

    /**
     * 所属 User 的 Outline
     */
    @Type(type = "json")
    @Column(name = "user_outline", columnDefinition = "json")
    private UserOutlineDto userOutline;
}

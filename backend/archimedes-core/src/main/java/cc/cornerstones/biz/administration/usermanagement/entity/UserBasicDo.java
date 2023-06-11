package cc.cornerstones.biz.administration.usermanagement.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import cc.cornerstones.biz.administration.usermanagement.share.constants.UserTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Where;

import javax.persistence.*;

/**
 * User basic
 *
 * @author bbottong
 */
@TinyId(bizType = UserBasicDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = UserBasicDo.RESOURCE_NAME,
        indexes = {
                @Index(name = "uid", columnList = "uid", unique = true),
                @Index(name = "display_name", columnList = "display_name", unique = false)
        })
@Where(clause = "is_deleted=0")
public class UserBasicDo extends BaseDo {
    public static final String RESOURCE_NAME = "k2_user_basic";
    public static final String RESOURCE_SYMBOL = "User basic";

    /**
     * UID
     */
    @Column(name = "uid")
    private Long uid;

    /**
     * Display name
     */
    @Column(name = "display_name", length = 64)
    private String displayName;

    /**
     * Enabled
     */
    @Column(name = "is_enabled")
    private Boolean enabled;

    /**
     * 用户类型
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "type")
    private UserTypeEnum type;
}
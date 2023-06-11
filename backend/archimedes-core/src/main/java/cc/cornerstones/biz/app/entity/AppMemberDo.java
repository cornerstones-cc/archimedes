package cc.cornerstones.biz.app.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import cc.cornerstones.biz.app.share.constants.AppMembershipEnum;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.*;

/**
 * App member
 *
 * @author bbottong
 */

@TypeDef(name = "json", typeClass = JsonStringType.class)
@TinyId(bizType = AppMemberDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = AppMemberDo.RESOURCE_NAME,
        indexes = {
                @Index(name = "app_uid_n_user_uid", columnList = "app_uid, user_uid", unique = false),
                @Index(name = "user_uid", columnList = "user_uid", unique = false),
        })
@Where(clause = "is_deleted=0")
public class AppMemberDo extends BaseDo {
    public static final String RESOURCE_NAME = "f1_app_member";
    public static final String RESOURCE_SYMBOL = "App member";

    /**
     * App's UID
     */
    @Column(name = "app_uid")
    private Long appUid;

    /**
     * App's member's membership
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "membership")
    private AppMembershipEnum membership;

    /**
     * App's member's user uid
     */
    @Column(name = "user_uid")
    private Long userUid;
}
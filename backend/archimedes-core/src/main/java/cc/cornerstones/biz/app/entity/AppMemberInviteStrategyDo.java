package cc.cornerstones.biz.app.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import cc.cornerstones.biz.app.share.types.InviteStrategy;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.*;

/**
 * App's member invite strategy
 *
 * @author bbottong
 */

@TypeDef(name = "json", typeClass = JsonStringType.class)
@TinyId(bizType = AppMemberInviteStrategyDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0,
        delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = AppMemberInviteStrategyDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class AppMemberInviteStrategyDo extends BaseDo {
    public static final String RESOURCE_NAME = "f1_app_member_invite_strategy";
    public static final String RESOURCE_SYMBOL = "App member invite strategy";

    /**
     * App's UID
     */
    @Column(name = "app_uid")
    private Long appUid;

    /**
     * App's member invite strategy
     */
    @Type(type = "json")
    @Column(name = "invite_strategy", columnDefinition = "json")
    private InviteStrategy inviteStrategy;
}
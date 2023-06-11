package cc.cornerstones.biz.app.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import cc.cornerstones.biz.app.share.types.GrantStrategy;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * App's member access grant strategy
 *
 * @author bbottong
 */

@TypeDef(name = "json", typeClass = JsonStringType.class)
@TinyId(bizType = AppMemberAccessGrantStrategyDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0,
        delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = AppMemberAccessGrantStrategyDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class AppMemberAccessGrantStrategyDo extends BaseDo {
    public static final String RESOURCE_NAME = "f1_app_member_access_grant_strategy";
    public static final String RESOURCE_SYMBOL = "App member access grant strategy";

    /**
     * App's UID
     */
    @Column(name = "app_uid")
    private Long appUid;

    /**
     * App's member access grant strategy
     */
    @Type(type = "json")
    @Column(name = "grant_strategy", columnDefinition = "json")
    private GrantStrategy grantStrategy;
}
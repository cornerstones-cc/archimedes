package cc.cornerstones.biz.app.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.*;

/**
 * App member access grant member
 *
 * @author bbottong
 */

@TypeDef(name = "json", typeClass = JsonStringType.class)
@TinyId(bizType = AppMemberAccessGrantMemberDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0,
        delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = AppMemberAccessGrantMemberDo.RESOURCE_NAME,
        indexes = {
                @Index(name = "app_uid_n_user_uid", columnList = "app_uid, user_uid", unique = false),
                @Index(name = "user_uid", columnList = "user_uid", unique = false),
        })
@Where(clause = "is_deleted=0")
public class AppMemberAccessGrantMemberDo extends BaseDo {
    public static final String RESOURCE_NAME = "f1_app_member_access_grant_member";
    public static final String RESOURCE_SYMBOL = "App member access grant member";

    /**
     * App's UID
     */
    @Column(name = "app_uid")
    private Long appUid;

    /**
     * App's data facet hierarchy 的 hierarchy node uid
     */
    @Column(name = "data_facet_hierarchy_node_uid")
    private Long dataFacetHierarchyNodeUid;

    /**
     * App's member's user uid
     */
    @Column(name = "user_uid")
    private Long userUid;
}
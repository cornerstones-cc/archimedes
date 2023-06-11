package cc.cornerstones.biz.administration.usermanagement.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import cc.cornerstones.biz.administration.usermanagement.share.constants.PermissionTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Where;

import javax.persistence.*;

/**
 * Group role
 *
 * @author bbottong
 */
@TinyId(bizType = GroupRoleDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = GroupRoleDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class GroupRoleDo extends BaseDo {
    public static final String RESOURCE_NAME = "k2_group_role";
    public static final String RESOURCE_SYMBOL = "Group role";

    /**
     * Role 的 UID
     */
    @Column(name = "role_uid")
    private Long roleUid;

    /**
     * Group 的 UID
     */
    @Column(name = "group_uid")
    private Long groupUid;
}
package cc.cornerstones.biz.administration.usermanagement.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import cc.cornerstones.biz.administration.usermanagement.share.constants.PermissionTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Where;

import javax.persistence.*;

/**
 * Role permission
 *
 * @author bbottong
 */
@TinyId(bizType = RolePermissionDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = RolePermissionDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class RolePermissionDo extends BaseDo {
    public static final String RESOURCE_NAME = "k2_role_permission";
    public static final String RESOURCE_SYMBOL = "Role permission";

    /**
     * Permission's UID
     */
    @Column(name = "permission_uid")
    private Long permissionUid;

    /**
     * Permission's type
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "permission_type")
    private PermissionTypeEnum permissionType;

    /**
     * Role 的 UID
     */
    @Column(name = "role_uid")
    private Long roleUid;
}
package cc.cornerstones.biz.administration.usermanagement.entity;

import cc.cornerstones.almond.types.BaseDo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * User role
 *
 * @author bbottong
 */
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = UserRoleDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class UserRoleDo extends BaseDo {
    public static final String RESOURCE_NAME = "k2_user_role";
    public static final String RESOURCE_SYMBOL = "User role";

    /**
     * Role UID
     */
    @Column(name = "role_uid")
    private Long roleUid;

    /**
     * 所属 User 的 UID
     */
    @Column(name = "user_uid")
    private Long userUid;
}
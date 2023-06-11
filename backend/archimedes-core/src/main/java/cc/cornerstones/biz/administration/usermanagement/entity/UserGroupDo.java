package cc.cornerstones.biz.administration.usermanagement.entity;

import cc.cornerstones.almond.types.BaseDo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * User group
 *
 * @author bbottong
 */
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = UserGroupDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class UserGroupDo extends BaseDo {
    public static final String RESOURCE_NAME = "k2_user_group";
    public static final String RESOURCE_SYMBOL = "User group";

    /**
     * Group UID
     */
    @Column(name = "group_uid")
    private Long groupUid;

    /**
     * 所属 User 的 UID
     */
    @Column(name = "user_uid")
    private Long userUid;
}
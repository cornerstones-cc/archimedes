package cc.cornerstones.biz.administration.usermanagement.entity;

import cc.cornerstones.almond.types.BaseDo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * User account
 *
 * @author bbottong
 */
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = UserAccountDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class UserAccountDo extends BaseDo {
    public static final String RESOURCE_NAME = "k2_user_account";
    public static final String RESOURCE_SYMBOL = "User account";

    /**
     * Name
     */
    @Column(name = "name", length = 129)
    private String name;

    /**
     * 所属 Account type 的 uid
     */
    @Column(name = "account_type_uid")
    private Long accountTypeUid;

    /**
     * 所属 User 的 UID
     */
    @Column(name = "user_uid")
    private Long userUid;
}
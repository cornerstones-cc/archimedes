package cc.cornerstones.biz.administration.usermanagement.entity;

import cc.cornerstones.almond.types.BaseDo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * User extended property
 *
 * @author bbottong
 */
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = UserExtendedPropertyDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class UserExtendedPropertyDo extends BaseDo {
    public static final String RESOURCE_NAME = "k2_user_extended_property";
    public static final String RESOURCE_SYMBOL = "User extended property";

    /**
     * Value
     */
    @Column(name = "extended_property_value", length = 255)
    private String extendedPropertyValue;

    /**
     * 所属 User schema extended property 的 UID
     */
    @Column(name = "extended_property_uid")
    private Long extendedPropertyUid;

    /**
     * 所属 User 的 UID
     */
    @Column(name = "user_uid")
    private Long userUid;
}
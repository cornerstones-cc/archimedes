package cc.cornerstones.biz.administration.usermanagement.entity;

import cc.cornerstones.almond.constants.DatabaseFieldTypeEnum;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * User schema extended property
 *
 * @author bbottong
 */
@TinyId(bizType = UserSchemaExtendedPropertyDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0,
        delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = UserSchemaExtendedPropertyDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class UserSchemaExtendedPropertyDo extends BaseDo {
    public static final String RESOURCE_NAME = "k2_user_schema_extended_property";
    public static final String RESOURCE_SYMBOL = "User schema extended property";

    /**
     * UID
     */
    @Column(name = "uid")
    private Long uid;

    /**
     * Name
     * <p>
     * A name is used to identify the object.
     */
    @Column(name = "name", length = 129)
    private String name;

    /**
     * Label
     * <p>
     * An object name is how the object is referenced programmatically.
     */
    @Column(name = "object_name", length = 150)
    private String objectName;

    /**
     * Description
     * <p>
     * A meaning description helps you remembers the differences between objects.
     */
    @Column(name = "description", length = 255)
    private String description;

    /**
     * Sequence
     */
    @Column(name = "sequence")
    private Float sequence;

    /**
     * Type
     */
    @Column(name = "type")
    private DatabaseFieldTypeEnum type;

    /**
     * Length
     */
    @Column(name = "length", length = 16)
    private String length;

    /**
     * Input validation regex
     */
    @Column(name = "input_validation_regex", length = 128)
    private String inputValidationRegex;

    /**
     * Nullable
     */
    @Column(name = "is_nullable")
    private Boolean nullable;

    /**
     * Show in filter
     */
    @Column(name = "is_show_in_filter")
    private Boolean showInFilter;

    /**
     * Show in detailed information
     */
    @Column(name = "is_show_in_detailed_information")
    private Boolean showInDetailedInformation;

    /**
     * Show in brief information
     */
    @Column(name = "is_show_in_brief_information")
    private Boolean showInBriefInformation;
}
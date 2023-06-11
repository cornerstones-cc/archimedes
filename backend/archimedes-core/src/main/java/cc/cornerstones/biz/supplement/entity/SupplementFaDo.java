package cc.cornerstones.biz.supplement.entity;

import cc.cornerstones.almond.types.BaseDo;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Supplement Fa
 *
 * @author bbottong
 */
@TypeDef(name = "json", typeClass = JsonStringType.class)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = SupplementFaDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class SupplementFaDo extends BaseDo {
    public static final String RESOURCE_NAME = "s9_supplement_fa";
    public static final String RESOURCE_SYMBOL = "Supplement Fa";

    /**
     * Operation name
     * 登录/查询/导出
     */
    @Column(name = "operation_name", length = 45)
    private String operationName;

    /**
     * Operation object uid
     */
    @Column(name = "operation_object_uid")
    private Long operationObjectUid;

    /**
     * Operation object code
     */
    @Column(name = "operation_object_code", length = 96)
    private String operationObjectCode;

    /**
     * Operation object name
     */
    @Column(name = "operation_object_name", length = 96)
    private String operationObjectName;

    /**
     * Operation created timestamp
     */
    @Column(name = "operation_created_timestamp")
    private LocalDateTime operationCreatedTimestamp;

    /**
     * Operation created date
     */
    @Column(name = "operation_created_date")
    private LocalDate operationCreatedDate;

    /**
     * Operation duration in seconds
     */
    @Column(name = "operation_duration_in_seconds")
    private Float operationDurationInSeconds;

    /**
     * Operation status
     * Optional values: FINISHED, FAILED
     */
    @Column(name = "operation_status")
    private String operationStatus;

    /**
     * Number of operation result records
     */
    @Column(name = "number_of_operation_result_records")
    private Long numberOfOperationResultRecords;

    /**
     * User uid
     */
    @Column(name = "user_uid")
    private Long userUid;

    /**
     * User code
     */
    @Column(name = "user_code", length = 64)
    private String userCode;

    /**
     * User name
     */
    @Column(name = "user_name", length = 96)
    private String userName;

    /**
     * User role uid(s)
     */
    @Column(name = "user_role_uid_list", length = 255)
    private String userRoleUidList;

    /**
     * User role name(s)
     */
    @Column(name = "user_role_name_list", length = 255)
    private String userRoleNameList;

    /**
     * Tester or not
     */
    @Column(name = "is_tester")
    private Boolean tester;

    /**
     * Reserved 0
     */
    @Column(name = "reserved_0", length = 255)
    private String reserved0;

    /**
     * Reserved 1
     */
    @Lob
    @Column(name = "reserved_1")
    private String reserved1;

    /**
     * Reserved 2
     */
    @Lob
    @Column(name = "reserved_2")
    private String reserved2;

    /**
     * Reserved 3
     */
    @Lob
    @Column(name = "reserved_3")
    private String reserved3;

    /**
     * Tracking number
     */
    @Column(name = "tracking_serial_number", length = 64)
    private String trackingSerialNumber;

    /**
     * Remark
     */
    @Column(name = "remark", length = 255)
    private String remark;
}
package cc.cornerstones.biz.operations.accesslogging.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import cc.cornerstones.biz.administration.usermanagement.dto.UserOutlineDto;
import com.alibaba.fastjson.JSONObject;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Query log
 *
 * @author bbottong
 */

@TypeDef(name = "json", typeClass = JsonStringType.class)
@TinyId(bizType = QueryLogDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = QueryLogDo.RESOURCE_NAME,
        indexes = {
                @Index(name = "tracking_serial_number", columnList = "tracking_serial_number", unique = false),
                @Index(name = "created_date_n_data_facet_uid", columnList = "created_date, data_facet_uid", unique =
                        false)
        })
@Where(clause = "is_deleted=0")
public class QueryLogDo extends BaseDo {
    public static final String RESOURCE_NAME = "f5_query_log";
    public static final String RESOURCE_SYMBOL = "Query log";

    /**
     * Tracking serial number
     */
    @Column(name = "tracking_serial_number", length = 36)
    private String trackingSerialNumber;

    /**
     * User UID
     */
    @Column(name = "user_uid")
    private Long userUid;

    /**
     * User display name
     */
    @Column(name = "display_name", length = 64)
    private String displayName;

    /**
     * Data facet uid
     */
    @Column(name = "data_facet_uid")
    private Long dataFacetUid;

    /**
     * Data facet name
     */
    @Column(name = "data_facet_name", length = 64)
    private String dataFacetName;

    /**
     * Successful
     */
    @Column(name = "is_successful")
    private Boolean successful;

    /**
     * Request
     */
    @Type(type = "json")
    @Column(name = "request", columnDefinition = "json")
    private JSONObject request;

    /**
     * Intermediate result
     */
    @Type(type = "json")
    @Column(name = "intermediate_result", columnDefinition = "json")
    private JSONObject intermediateResult;

    /**
     * Response
     */
    @Type(type = "json")
    @Column(name = "response", columnDefinition = "json")
    private JSONObject response;

    /**
     * Count Statement
     */
    @Lob
    @Column(name = "count_statement")
    private String countStatement;

    /**
     * Query Statement
     */
    @Lob
    @Column(name = "query_statement")
    private String queryStatement;

    /**
     * 开始时间戳
     */
    @Column(name = "begin_timestamp")
    private LocalDateTime beginTimestamp;

    /**
     * Count 结束时间戳
     */
    @Column(name = "count_end_timestamp")
    private LocalDateTime countEndTimestamp;

    /**
     * Query 结束时间戳
     */
    @Column(name = "query_end_timestamp")
    private LocalDateTime queryEndTimestamp;

    /**
     * 结束时间戳
     */
    @Column(name = "end_timestamp")
    private LocalDateTime endTimestamp;

    /**
     * 源数据中总行数
     */
    @Column(name = "total_rows_in_source")
    private Long totalRowsInSource;

    /**
     * 源数据中总列数
     */
    @Column(name = "total_columns_in_source")
    private Integer totalColumnsInSource;

    /**
     * 累计 Count 时长（秒）
     */
    @Column(name = "count_duration_in_millis")
    private Long countDurationInMillis;

    /**
     * 累计 Count 时长（自适应单位：时/分/秒）
     */
    @Column(name = "count_duration_remark", length = 45)
    private String countDurationRemark;

    /**
     * 累计 Query 时长（秒）
     */
    @Column(name = "query_duration_in_millis")
    private Long queryDurationInMillis;

    /**
     * 累计 Query 时长（自适应单位：时/分/秒）
     */
    @Column(name = "query_duration_remark", length = 45)
    private String queryDurationRemark;

    /**
     * 累计总时长（秒）
     */
    @Column(name = "total_duration_in_millis")
    private Long totalDurationInMillis;

    /**
     * 累计总时长（自适应单位：时/分/秒）
     */
    @Column(name = "total_duration_remark", length = 45)
    private String totalDurationRemark;

    /**
     * Remark
     */
    @Column(name = "remark", length = 255)
    private String remark;

    /**
     * Created date
     */
    @Column(name = "created_date")
    private LocalDate createdDate;

    /**
     * 所属 User 的 Outline
     */
    @Type(type = "json")
    @Column(name = "user_outline", columnDefinition = "json")
    private UserOutlineDto userOutline;
}
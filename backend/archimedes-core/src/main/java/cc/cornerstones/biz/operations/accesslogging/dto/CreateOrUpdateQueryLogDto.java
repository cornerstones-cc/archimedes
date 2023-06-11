package cc.cornerstones.biz.operations.accesslogging.dto;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import java.time.LocalDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class CreateOrUpdateQueryLogDto {

    /**
     * Tracking serial number
     */
    private String trackingSerialNumber;

    /**
     * User UID
     */
    private Long userUid;

    /**
     * User display name
     */
    private String displayName;

    /**
     * Data facet uid
     */
    private Long dataFacetUid;

    /**
     * Data facet name
     */
    private String dataFacetName;

    /**
     * Successful
     */
    private Boolean successful;

    /**
     * Request
     */
    private JSONObject request;

    /**
     * Intermediate result
     */
    private JSONObject intermediateResult;

    /**
     * Response
     */
    private JSONObject response;

    /**
     * Count Statement
     */
    private String countStatement;

    /**
     * Query Statement
     */
    private String queryStatement;

    /**
     * 开始时间戳
     */
    private LocalDateTime beginTimestamp;

    /**
     * 结束时间戳
     */
    private LocalDateTime endTimestamp;

    /**
     * 源数据中总行数
     */
    private Long totalRowsInSource;

    /**
     * 源数据中总列数
     */
    private Integer totalColumnsInSource;

    /**
     * 累计 Count 时长（毫秒）
     */
    private Long countDurationInMillis;

    /**
     * 累计 Count 时长（自适应单位：时/分/秒）
     */
    private String countDurationRemark;

    /**
     * 累计 Query 时长（毫秒）
     */
    private Long queryDurationInMillis;

    /**
     * 累计 Query 时长（自适应单位：时/分/秒）
     */
    private String queryDurationRemark;

    /**
     * 累计总时长（毫秒）
     */
    private Long totalDurationInMillis;

    /**
     * 累计总时长（自适应单位：时/分/秒）
     */
    private String totalDurationRemark;

    /**
     * Remark
     */
    private String remark;
}

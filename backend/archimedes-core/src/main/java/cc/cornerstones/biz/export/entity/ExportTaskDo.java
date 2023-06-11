package cc.cornerstones.biz.export.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.biz.administration.usermanagement.dto.UserOutlineDto;
import cc.cornerstones.biz.export.share.constants.ExportTaskStatusEnum;
import cc.cornerstones.biz.export.share.constants.FileFormatEnum;
import cc.cornerstones.biz.share.constants.ExportOptionEnum;
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
 * Export task
 *
 * @author bbottong
 */
@TypeDef(name = "json", typeClass = JsonStringType.class)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = ExportTaskDo.RESOURCE_NAME,
        indexes = {
                @Index(name = "task_uid_n_task_status", columnList = "task_uid, task_status", unique = false),
                @Index(name = "created_date_n_data_facet_uid", columnList = "created_date, data_facet_uid", unique =
                        false)
        })
@Where(clause = "is_deleted=0")
public class ExportTaskDo extends BaseDo {
    public static final String RESOURCE_NAME = "t5_export_task";
    public static final String RESOURCE_SYMBOL = "Export task";

    /**
     * Task UID
     */
    @Column(name = "task_uid")
    private Long taskUid;

    /**
     * Task Name
     */
    @Column(name = "task_name")
    private String taskName;

    /**
     * Task status
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "task_status")
    private ExportTaskStatusEnum taskStatus;

    /**
     * Export option
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "export_option")
    private ExportOptionEnum exportOption;

    /**
     * Intermediate result
     */
    @Type(type = "json")
    @Column(name = "intermediate_result", columnDefinition = "json")
    private JSONObject intermediateResult;

    /**
     * Query Statement
     */
    @Lob
    @Column(name = "query_statement")
    private String queryStatement;

    /**
     * Count Statement
     */
    @Lob
    @Column(name = "count_statement")
    private String countStatement;

    /**
     * 开始时间戳
     */
    @Column(name = "begin_timestamp")
    private LocalDateTime beginTimestamp;

    /**
     * Count 开始时间戳
     */
    @Column(name = "count_begin_timestamp")
    private LocalDateTime countBeginTimestamp;

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
     * Fetch (读数及写入本地文件) 结束时间戳，status 变成 PRODUCED 的时间戳
     */
    @Column(name = "fetch_end_timestamp")
    private LocalDateTime fetchEndTimestamp;

    /**
     * Transfer (转移本地文件到持久文件存储系统) 结束时间戳，status 变成 TRANSFERRED 的时间戳
     */
    @Column(name = "transfer_end_timestamp")
    private LocalDateTime transferEndTimestamp;

    /**
     * 结束时间戳，根据 status 可以是 finished, failed, canceled timestamp
     */
    @Column(name = "end_timestamp")
    private LocalDateTime endTimestamp;

    /**
     * Fetch 执行进展百分比，执行到第几页，总共多少页，百分比
     */
    @Column(name = "fetch_progress_percentage", length = 15)
    private String fetchProgressPercentage;

    /**
     * Fetch 执行进展备注，执行到第几页，总共多少页
     */
    @Column(name = "fetch_progress_remark", length = 45)
    private String fetchProgressRemark;

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
     * （持久）文件在哪个 DFS service agent
     */
    @Column(name = "dfs_service_agent_uid")
    private Long dfsServiceAgentUid;

    /**
     * （持久）文件ID
     */
    @Column(name = "file_id", length = 255)
    private String fileId;

    /**
     * 文件名称
     */
    @Column(name = "file_name", length = 255)
    private String fileName;

    /**
     * 文件中总行数
     */
    @Column(name = "total_rows_in_file")
    private Long totalRowsInFile;

    /**
     * 文件大小（字节）
     */
    @Column(name = "file_length_in_bytes")
    private Long fileLengthInBytes;

    /**
     * 文件大小备注（自适应单位：GB/MB/KB/B）
     */
    @Column(name = "file_length_remark", length = 45)
    private String fileLengthRemark;

    /**
     * 累计 Count 时长（秒）
     */
    @Column(name = "total_count_duration_in_secs")
    private Long totalCountDurationInSecs;

    /**
     * 累计 Query 时长（秒）
     */
    @Column(name = "total_query_duration_in_secs")
    private Long totalQueryDurationInSecs;

    /**
     * 累计 Fetch (read) 时长（秒），fetch = read + write
     */
    @Column(name = "total_read_duration_in_secs")
    private Long totalReadDurationInSecs;

    /**
     * 累计 Fetch (write) 时长（秒），fetch = read + write
     */
    @Column(name = "total_write_duration_in_secs")
    private Long totalWriteDurationInSecs;

    /**
     * 累计 Transfer 时长（秒）
     */
    @Column(name = "total_transfer_duration_in_secs")
    private Long totalTransferDurationInSecs;

    /**
     * 累计总时长（秒）
     */
    @Column(name = "total_duration_in_secs")
    private Long totalDurationInSecs;

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
     * Data facet uid
     */
    @Column(name = "data_facet_uid")
    private Long dataFacetUid;

    /**
     * Data facet name
     */
    @Column(name = "data_facet_name", length = 129)
    private String dataFacetName;

    /**
     * 所属 User 的 Outline
     */
    @Type(type = "json")
    @Column(name = "user_outline", columnDefinition = "json")
    private UserOutlineDto userOutline;
}
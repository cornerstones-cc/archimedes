package cc.cornerstones.biz.export.dto;

import cc.cornerstones.almond.types.BaseDto;
import cc.cornerstones.biz.export.share.constants.ExportTaskStatusEnum;
import cc.cornerstones.biz.share.constants.ExportOptionEnum;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Export task
 *
 * @author bbottong
 */
@Data
public class ExportTaskDto extends BaseDto {

    /**
     * Task UID
     */
    private Long taskUid;

    /**
     * Task Name
     */
    private String taskName;

    /**
     * Task status
     */
    private ExportTaskStatusEnum taskStatus;

    /**
     * Export option
     */
    private ExportOptionEnum exportOption;

    /**
     * Query Statement
     */
    private String queryStatement;

    /**
     * Count Statement
     */
    private String countStatement;

    /**
     * 开始时间戳
     */
    private LocalDateTime beginTimestamp;

    /**
     * Count 开始时间戳
     */
    private LocalDateTime countBeginTimestamp;

    /**
     * Count 结束时间戳
     */
    private LocalDateTime countEndTimestamp;

    /**
     * Query 结束时间戳
     */
    private LocalDateTime queryEndTimestamp;

    /**
     * Fetch (读数及写入本地文件) 结束时间戳，status 变成 PRODUCED 的时间戳
     */
    private LocalDateTime fetchEndTimestamp;

    /**
     * Transfer (转移本地文件到持久文件存储系统) 结束时间戳，status 变成 TRANSFERRED 的时间戳
     */
    private LocalDateTime transferEndTimestamp;

    /**
     * 结束时间戳，根据 status 可以是 finished, failed, canceled timestamp
     */
    private LocalDateTime endTimestamp;

    /**
     * Fetch 执行进展百分比，执行到第几页，总共多少页，百分比
     */
    private String fetchProgressPercentage;

    /**
     * Fetch 执行进展备注，执行到第几页，总共多少页
     */
    private String fetchProgressRemark;

    /**
     * 源数据中总行数
     */
    private Long totalRowsInSource;

    /**
     * 源数据中总列数
     */
    private Integer totalColumnsInSource;

    /**
     * （持久）文件在哪个 DFS service component
     */
    private Long dfsServiceComponentUid;

    /**
     * （持久）文件ID
     */
    private String fileId;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件中总行数
     */
    private Long totalRowsInFile;

    /**
     * 文件大小（字节）
     */
    private Long fileLengthInBytes;

    /**
     * 文件大小备注（自适应单位：GB/MB/KB/B）
     */
    private String fileLengthRemark;

    /**
     * 累计 Count 时长（秒）
     */
    private Long totalCountDurationInSecs;

    /**
     * 累计 Query 时长（秒）
     */
    private Long totalQueryDurationInSecs;

    /**
     * 累计 Fetch (read) 时长（秒），fetch = read + write
     */
    private Long totalReadDurationInSecs;

    /**
     * 累计 Fetch (write) 时长（秒），fetch = read + write
     */
    private Long totalWriteDurationInSecs;

    /**
     * 累计 Transfer 时长（秒）
     */
    private Long totalTransferDurationInSecs;

    /**
     * 累计总时长（秒）
     */
    private Long totalDurationInSecs;

    /**
     * 累计总时长（自适应单位：时/分/秒）
     */
    private String totalDurationRemark;

    /**
     * Remark
     */
    private String remark;

    /**
     * Created date
     */
    private LocalDate createdDate;

    /**
     * Data facet uid
     */
    private Long dataFacetUid;
}
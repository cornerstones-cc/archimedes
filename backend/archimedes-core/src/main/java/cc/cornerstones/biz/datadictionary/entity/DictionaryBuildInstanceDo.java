package cc.cornerstones.biz.datadictionary.entity;

import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Dictionary build instance
 *
 * @author bbottong
 */

@TypeDef(name = "json", typeClass = JsonStringType.class)
@TinyId(bizType = DictionaryBuildInstanceDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0,
        delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = DictionaryBuildInstanceDo.RESOURCE_NAME,
        indexes = {
                @Index(name = "dictionary_category_uid", columnList = "dictionary_category_uid", unique = false)
        })
@Where(clause = "is_deleted=0")
public class DictionaryBuildInstanceDo extends BaseDo {
    public static final String RESOURCE_NAME = "a3_dt_dictionary_build_instance";
    public static final String RESOURCE_SYMBOL = "Data dictionary build instance";

    /**
     * UID
     */
    @Column(name = "uid")
    private Long uid;

    /**
     * Status
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status")
    private JobStatusEnum status;

    /**
     * 开始时间戳
     */
    @Column(name = "started_timestamp")
    private LocalDateTime startedTimestamp;

    /**
     * 完成时间戳
     */
    @Column(name = "finished_timestamp")
    private LocalDateTime finishedTimestamp;

    /**
     * 失败时间戳
     */
    @Column(name = "failed_timestamp")
    private LocalDateTime failedTimestamp;

    /**
     * 取消时间戳
     */
    @Column(name = "canceled_timestamp")
    private LocalDateTime canceledTimestamp;

    /**
     * 所属 Dictionary Category (字典类目) 的 UID
     */
    @Column(name = "dictionary_category_uid")
    private Long dictionaryCategoryUid;

    @Column(name = "remark", length = 255)
    private String remark;
}
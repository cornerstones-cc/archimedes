package cc.cornerstones.biz.datatable.entity;


import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.List;

/**
 * Data Table (Context Path) Metadata Retrieval Instance
 *
 * @author bbottong
 */

@TypeDef(name = "json", typeClass = JsonStringType.class)
@TinyId(bizType = ContextPathMetadataRetrievalInstanceDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100,
        remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = ContextPathMetadataRetrievalInstanceDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class ContextPathMetadataRetrievalInstanceDo extends BaseDo {
    public static final String RESOURCE_NAME = "a3_dt_metadata_retrieval_context_path";
    public static final String RESOURCE_SYMBOL = "Data table (context path) metadata retrieval instance";

    /**
     * UID
     */
    @Column(name = "uid")
    private Long uid;

    /**
     * Type
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status")
    private JobStatusEnum status;

    /**
     * Remark
     */
    @Column(name = "remark", length = 255)
    private String remark;

    /**
     * Context Path
     * '/' 分隔路径
     */
    @Column(name = "context_path_str", length = 255)
    private String contextPathStr;

    /**
     * Context Path
     */
    @Type(type = "json")
    @Column(name = "context_path", columnDefinition = "json")
    private List<String> contextPath;

    /**
     * 所属 Data Source 的 UID
     */
    @Column(name = "data_source_uid")
    private Long dataSourceUid;
}
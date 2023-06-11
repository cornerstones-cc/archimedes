package cc.cornerstones.biz.datasource.entity;


import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.*;

/**
 * Data Source Metadata Retrieval Instance
 *
 * @author bbottong
 */

@TypeDef(name = "json", typeClass = JsonStringType.class)
@TinyId(bizType = DataSourceMetadataRetrievalInstanceDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100,
        remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = DataSourceMetadataRetrievalInstanceDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class DataSourceMetadataRetrievalInstanceDo extends BaseDo {
    public static final String RESOURCE_NAME = "a3_ds_metadata_retrieval";
    public static final String RESOURCE_SYMBOL = "Data source metadata retrieval instance";

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
     * 所属 Data Source 的 UID
     */
    @Column(name = "data_source_uid")
    private Long dataSourceUid;
}
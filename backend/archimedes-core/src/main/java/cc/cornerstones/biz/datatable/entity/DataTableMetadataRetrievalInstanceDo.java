package cc.cornerstones.biz.datatable.entity;


import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Where;

import javax.persistence.*;

/**
 * Data Table Metadata Retrieval Instance
 *
 * @author bbottong
 */

@TinyId(bizType = DataTableMetadataRetrievalInstanceDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100,
        remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = DataTableMetadataRetrievalInstanceDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class DataTableMetadataRetrievalInstanceDo extends BaseDo {
    public static final String RESOURCE_NAME = "a3_dt_metadata_retrieval_data_table";
    public static final String RESOURCE_SYMBOL = "Data table metadata retrieval instance";

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
     * Data table
     */
    @Column(name = "data_table_uid")
    private Long dataTableUid;

    /**
     * 所属 Data Source 的 UID
     */
    @Column(name = "data_source_uid")
    private Long dataSourceUid;
}
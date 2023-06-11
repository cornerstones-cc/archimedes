package cc.cornerstones.biz.datatable.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import cc.cornerstones.biz.datasource.share.constants.DataColumnTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Where;

import javax.persistence.*;

/**
 * Data Column
 *
 * @author bbottong
 */
@TinyId(bizType = DataColumnDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = DataColumnDo.RESOURCE_NAME,
        indexes = {
                @Index(name = "data_table_uid", columnList = "data_table_uid",
                        unique = false)
        })
@Where(clause = "is_deleted=0")
public class DataColumnDo extends BaseDo {
    public static final String RESOURCE_NAME = "a3_dt_data_column";
    public static final String RESOURCE_SYMBOL = "Data column";

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
     * Object Name
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
     * Type
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "type")
    private DataColumnTypeEnum type;

    /**
     * Ordinal Position
     *
     * 在所有字段中的序号（从0开始计数）
     */
    @Column(name = "ordinal_position")
    private Float ordinalPosition;

    /**
     * 所属 Data Table 的 UID
     */
    @Column(name = "data_table_uid")
    private Long dataTableUid;

    /**
     * 所属 Data Source 的 UID
     */
    @Column(name = "data_source_uid")
    private Long dataSourceUid;
}
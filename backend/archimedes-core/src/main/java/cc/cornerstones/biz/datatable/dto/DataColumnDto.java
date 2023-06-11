package cc.cornerstones.biz.datatable.dto;

import cc.cornerstones.almond.types.BaseDto;
import cc.cornerstones.biz.datasource.share.constants.DataColumnTypeEnum;
import lombok.Data;

/**
 * Data Column
 *
 * @author bbottong
 *
 */
@Data
public class DataColumnDto extends BaseDto {

    private Long uid;

    /**
     * Name
     * <p>
     * A name is used to identify the object.
     */
    private String name;

    /**
     * Object Name
     * <p>
     * An object name is how the object is referenced programmatically.
     */
    private String objectName;

    /**
     * Description
     * <p>
     * A meaning description helps you remembers the differences between objects.
     */
    private String description;

    /**
     * Type
     */
    private DataColumnTypeEnum type;

    /**
     * Ordinal Position
     *
     * 在所有字段中的序号（从0开始计数）
     */
    private Integer ordinalPosition;

    /**
     * 所属 Data Table 的 UID
     */
    private Long dataTableUid;

    /**
     * 所属 Data Source 的 UID
     */
    private Long dataSourceUid;
}
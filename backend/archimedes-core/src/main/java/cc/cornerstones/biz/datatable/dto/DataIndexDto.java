package cc.cornerstones.biz.datatable.dto;

import cc.cornerstones.almond.types.BaseDto;
import lombok.Data;

import java.util.List;

/**
 * Data Index
 *
 * @author bbottong
 *
 */
@Data
public class DataIndexDto extends BaseDto {

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
     * Unique
     */
    private Boolean unique;

    /**
     * Columns
     */
    private List<String> columns;

    /**
     * 所属 Data Table 的 UID
     */
    private Long dataTableUid;

    /**
     * 所属 Data Source 的 UID
     */
    private Long dataSourceUid;
}
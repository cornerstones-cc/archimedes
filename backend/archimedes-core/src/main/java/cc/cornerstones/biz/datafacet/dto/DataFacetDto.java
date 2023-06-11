package cc.cornerstones.biz.datafacet.dto;

import cc.cornerstones.almond.types.BaseDto;
import cc.cornerstones.biz.datasource.dto.DataSourceSimpleDto;
import cc.cornerstones.biz.datatable.dto.DataTableSimpleDto;
import lombok.Data;

@Data
public class DataFacetDto extends BaseDto {
    /**
     * UID
     */
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
     */
    private String description;

    /**
     * Enabled
     */
    private Boolean enabled;

    /**
     * Remark
     */
    private String remark;

    /**
     * 内部版本号
     */
    private Long buildNumber;

    /**
     * 所依托的 data table
     */
    private DataTableSimpleDto dataTable;

    /**
     * 所依托的 data source
     */
    private DataSourceSimpleDto dataSource;
}

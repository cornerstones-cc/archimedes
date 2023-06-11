package cc.cornerstones.biz.datatable.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import cc.cornerstones.biz.datasource.share.constants.DataTableTypeEnum;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.List;

/**
 * Data Table
 *
 * @author bbottong
 */

@TypeDef(name = "json", typeClass = JsonStringType.class)
@TinyId(bizType = DataTableDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = DataTableDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class DataTableDo extends BaseDo {
    public static final String RESOURCE_NAME = "a3_dt_data_table";
    public static final String RESOURCE_SYMBOL = "Data table";

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
    private DataTableTypeEnum type;

    /**
     * Context Path
     */
    @Column(name = "context_path_str", length = 255)
    private String contextPathStr;

    /**
     * 当 type = DataTableTypeEnum.DATABASE_TABLE or DataTableTypeEnum.DATABASE_VIEW 时，需要填写
     * Context Path
     */
    @Type(type = "json")
    @Column(name = "context_path", columnDefinition = "json")
    private List<String> contextPath;

    /**
     * 当 type = DataTableTypeEnum.INDIRECT_TABLE 时，需要填写
     * Building Logic of indirect table
     */
    @Lob
    @Column(name = "building_logic")
    private String buildingLogic;

    /**
     * 当 type = DataTableTypeEnum.INDIRECT_TABLE 时，需要程序自动解析出 underlying data table(s)
     */
    @Type(type = "json")
    @Column(name = "underlying_data_table_uid_list", columnDefinition = "json")
    private List<Long> underlyingDataTableUidList;

    /**
     * 内部版本号
     */
    @Column(name = "build_number")
    private Long buildNumber;

    /**
     * 所属 Data Source 的 UID
     */
    @Column(name = "data_source_uid")
    private Long dataSourceUid;
}
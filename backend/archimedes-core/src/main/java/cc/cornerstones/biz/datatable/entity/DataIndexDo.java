package cc.cornerstones.biz.datatable.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.List;

/**
 * Data Index
 *
 * @author bbottong
 */

@TypeDef(name = "json", typeClass = JsonStringType.class)
@TinyId(bizType = DataIndexDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = DataIndexDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class DataIndexDo extends BaseDo {
    public static final String RESOURCE_NAME = "a3_dt_data_index";
    public static final String RESOURCE_SYMBOL = "Data index";

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
     * Unique
     */
    @Column(name = "is_unique", columnDefinition = "boolean default false")
    private Boolean unique;

    /**
     * Columns
     * list item 的顺序即为 column 在 index 中的顺序
     */
    @Type(type = "json")
    @Column(name = "columns", columnDefinition = "json")
    private List<String> columns;

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
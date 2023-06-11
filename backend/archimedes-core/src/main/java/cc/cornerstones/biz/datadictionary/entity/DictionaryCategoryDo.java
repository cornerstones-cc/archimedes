package cc.cornerstones.biz.datadictionary.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.*;

/**
 * Dictionary's Category
 * 字典类目。
 * 字典由三个概念组成：一个或多个字典类目，每个字典类目的一级或者多级结构，每个字典类目的按结构组织的一个或多个键值对。
 *
 * @author bbottong
 */

@TypeDef(name = "json", typeClass = JsonStringType.class)
@TinyId(bizType = DictionaryCategoryDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = DictionaryCategoryDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class DictionaryCategoryDo extends BaseDo {
    public static final String RESOURCE_NAME = "a3_dt_dictionary_category";
    public static final String RESOURCE_SYMBOL = "Data dictionary category";

    /**
     * 为了适应前端显示，标记type
     */
    public static final String TYPE = "category";

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
     * Sequence
     */
    @Column(name = "sequence")
    private Float sequence;

    /**
     * Directory
     */
    @Column(name = "is_directory")
    private Boolean directory;

    /**
     * Version
     */
    @Column(name = "version")
    private Long version;

    /**
     * Parent 的 UID
     */
    @Column(name = "parent_uid")
    private Long parentUid;
}
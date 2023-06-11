package cc.cornerstones.biz.datadictionary.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Structure Node of Dictionary's Category
 *
 * @author bbottong
 */

@TypeDef(name = "json", typeClass = JsonStringType.class)
@TinyId(bizType = DictionaryStructureNodeDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0,
        delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = DictionaryStructureNodeDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class DictionaryStructureNodeDo extends BaseDo {
    public static final String RESOURCE_NAME = "a3_dt_dictionary_structure_node";
    public static final String RESOURCE_SYMBOL = "Data dictionary structure node";

    /**
     * 为了适应前端显示，标记type
     */
    public static final String TYPE = "structure";

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
     * 结构树中上一级节点的UID
     */
    @Column(name = "parent_uid")
    private Long parentUid;

    /**
     * 所属 Dictionary Category (字典类目) 的 UID
     */
    @Column(name = "dictionary_category_uid")
    private Long dictionaryCategoryUid;
}
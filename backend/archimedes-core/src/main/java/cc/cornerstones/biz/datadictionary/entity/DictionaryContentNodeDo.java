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
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * Content Node of Dictionary's Category
 *
 * @author bbottong
 */
@TypeDef(name = "json", typeClass = JsonStringType.class)
@TinyId(bizType = DictionaryContentNodeDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0, delta
        = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = DictionaryContentNodeDo.RESOURCE_NAME,
        indexes = {
                @Index(name = "dictionary_category_uid_n_version", columnList = "dictionary_category_uid, version", unique = false)
        })
@Where(clause = "is_deleted=0")
public class DictionaryContentNodeDo extends BaseDo {
    public static final String RESOURCE_NAME = "a3_dt_dictionary_content_node";
    public static final String RESOURCE_SYMBOL = "Data dictionary content node";

    /**
     * 为了适应前端显示，标记type
     */
    public static final String TYPE = "content";

    /**
     * UID
     */
    @Column(name = "uid")
    private Long uid;

    /**
     * Value
     */
    @Column(name = "value", length = 255)
    private String value;

    /**
     * Symbol
     */
    @Column(name = "label", length = 255)
    private String label;

    /**
     * Label (Physical)
     */
    @Column(name = "label_physical", length = 255)
    private String labelPhysical;

    /**
     * Label (Logical)
     */
    @Column(name = "label_logical", length = 255)
    private String labelLogical;

    /**
     * 内容树中上一级节点的ID
     */
    @Column(name = "parent_uid")
    private Long parentUid;

    /**
     * Sequence
     */
    @Column(name = "sequence")
    private Float sequence;

    /**
     * Version
     */
    @Column(name = "version")
    private Long version;

    /**
     * 指定字典类目的UID
     */
    @Column(name = "dictionary_category_uid")
    private Long dictionaryCategoryUid;
}

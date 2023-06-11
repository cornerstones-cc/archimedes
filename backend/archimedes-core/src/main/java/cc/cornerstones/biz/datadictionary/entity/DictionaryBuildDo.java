package cc.cornerstones.biz.datadictionary.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.biz.datatable.share.constants.DictionaryBuildTypeEnum;
import com.alibaba.fastjson.JSONObject;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.*;

/**
 * Dictionary build
 *
 * 一个 dictionary category 可以有0个或1个 dictionary build
 *
 * @author bbottong
 */

@TypeDef(name = "json", typeClass = JsonStringType.class)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = DictionaryBuildDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class DictionaryBuildDo extends BaseDo {
    public static final String RESOURCE_NAME = "a3_dt_dictionary_build";
    public static final String RESOURCE_SYMBOL = "Data dictionary build";

    /**
     * Enabled
     */
    @Column(name = "is_enabled")
    private Boolean enabled;

    /**
     * CRON Expression
     */
    @Column(name = "cron_expression")
    private String cronExpression;

    /**
     * Type
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "type")
    private DictionaryBuildTypeEnum type;

    /**
     * Logic
     */
    @Type(type = "json")
    @Column(name = "logic", columnDefinition = "json")
    private JSONObject logic;

    /**
     * Job UID
     */
    @Column(name = "job_uid")
    private Long jobUid;

    /**
     * 所属 Dictionary Category (字典类目) 的 UID
     */
    @Column(name = "dictionary_category_uid")
    private Long dictionaryCategoryUid;
}
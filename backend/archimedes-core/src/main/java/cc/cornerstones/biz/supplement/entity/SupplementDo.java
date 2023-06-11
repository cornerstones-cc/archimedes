package cc.cornerstones.biz.supplement.entity;

import cc.cornerstones.almond.types.BaseDo;
import com.alibaba.fastjson.JSONObject;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.*;

/**
 * Supplement
 *
 * @author bbottong
 */
@TypeDef(name = "json", typeClass = JsonStringType.class)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = SupplementDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class SupplementDo extends BaseDo {
    public static final String RESOURCE_NAME = "s9_supplement";
    public static final String RESOURCE_SYMBOL = "Supplement";

    /**
     * Name
     * <p>
     * A name is used to identify the object.
     */
    @Column(name = "name", length = 129)
    private String name;

    /**
     * Enabled
     */
    @Column(name = "is_enabled")
    private Boolean enabled;

    /**
     * Configuration
     */
    @Type(type = "json")
    @Column(name = "configuration", columnDefinition = "json")
    private JSONObject configuration;

    /**
     * Context
     */
    @Type(type = "json")
    @Column(name = "context", columnDefinition = "json")
    private JSONObject context;
}
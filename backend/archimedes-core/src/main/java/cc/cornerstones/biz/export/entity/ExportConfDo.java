package cc.cornerstones.biz.export.entity;

import cc.cornerstones.almond.types.BaseDo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Where;

import javax.persistence.*;

/**
 * Export Conf
 *
 * @author bbottong
 */
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = ExportConfDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class ExportConfDo extends BaseDo {
    public static final String RESOURCE_NAME = "t5_export_conf";
    public static final String RESOURCE_SYMBOL = "Export conf";

    /**
     * Property Name
     */
    @Column(name = "property_name")
    private String propertyName;

    /**
     * Property Value
     */
    @Column(name = "property_value")
    private String propertyValue;
}
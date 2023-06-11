package cc.cornerstones.biz.administration.systemsettings.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Settings
 *
 */
@TinyId(bizType = SettingsDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = SettingsDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class SettingsDo extends BaseDo {
    public static final String RESOURCE_NAME = "k1_settings";
    public static final String RESOURCE_SYMBOL = "settings";

    @Column(name = "name")
    private String name;

    @Column(name = "value")
    private String value;
}

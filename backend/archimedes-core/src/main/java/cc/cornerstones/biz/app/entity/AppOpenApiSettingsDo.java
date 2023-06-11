package cc.cornerstones.biz.app.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.biz.datafacet.dto.AppOpenApiSettingsContentDto;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * App open api settings
 *
 * @author bbottong
 */

@TypeDef(name = "json", typeClass = JsonStringType.class)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = AppOpenApiSettingsDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class AppOpenApiSettingsDo extends BaseDo {
    public static final String RESOURCE_NAME = "f1_app_open_api_settings";
    public static final String RESOURCE_SYMBOL = "App open api settings";

    /**
     * Content
     *
     */
    @Type(type = "json")
    @Column(name = "content", columnDefinition = "json")
    private AppOpenApiSettingsContentDto content;

    /**
     * 所属 App 的 UID
     */
    @Column(name = "app_uid")
    private Long appUid;
}
package cc.cornerstones.biz.datafacet.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import cc.cornerstones.biz.datafacet.share.constants.ExportExtendedTemplateVisibilityEnum;
import cc.cornerstones.biz.datafacet.share.constants.TemplateColumnHeaderSourceEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Where;

import javax.persistence.*;

/**
 * Export extended template
 *
 * @author bbottong
 */
@TinyId(bizType = ExportExtendedTemplateDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0,
        delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = ExportExtendedTemplateDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class ExportExtendedTemplateDo extends BaseDo {
    public static final String RESOURCE_NAME = "a1_df_appearance_export_extended_template";
    public static final String RESOURCE_SYMBOL = "Export extended template";

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
     * Enabled
     */
    @Column(name = "is_enabled", columnDefinition = "boolean default true")
    private Boolean enabled;

    /**
     * File
     */
    @Column(name = "file_id", length = 255)
    private String fileId;

    /**
     * DFS service agent uid
     */
    @Column(name = "dfs_service_agent_uid")
    private Long dfsServiceAgentUid;

    /**
     * Visibility
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "visibility")
    private ExportExtendedTemplateVisibilityEnum visibility;

    /**
     * Column header source
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "column_header_source")
    private TemplateColumnHeaderSourceEnum columnHeaderSource;

    /**
     * 所属 Data Facet 的 UID
     */
    @Column(name = "data_facet_uid")
    private Long dataFacetUid;
}
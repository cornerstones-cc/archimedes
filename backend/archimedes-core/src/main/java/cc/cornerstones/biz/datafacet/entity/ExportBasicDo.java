package cc.cornerstones.biz.datafacet.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.biz.export.share.constants.ExportCsvStrategyEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Where;

import javax.persistence.*;

/**
 * Export basic
 *
 * @author bbottong
 */
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = ExportBasicDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class ExportBasicDo extends BaseDo {
    public static final String RESOURCE_NAME = "a1_df_appearance_export_basic";
    public static final String RESOURCE_SYMBOL = "Export basic";

    /**
     * Enabled export csv
     */
    @Column(name = "is_enabled_export_csv", columnDefinition = "boolean default true")
    private Boolean enabledExportCsv;

    /**
     * If enabled export csv, what the export csv strategy is
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "export_csv_strategy")
    private ExportCsvStrategyEnum exportCsvStrategy;

    /**
     * Enabled export excel
     */
    @Column(name = "is_enabled_export_excel", columnDefinition = "boolean default true")
    private Boolean enabledExportExcel;

    /**
     * Enabled export data and images
     */
    @Column(name = "is_enabled_export_data_and_images", columnDefinition = "boolean default false")
    private Boolean enabledExportDataAndImages;

    /**
     * Enabled export data and files
     */
    @Column(name = "is_enabled_export_data_and_files", columnDefinition = "boolean default false")
    private Boolean enabledExportDataAndFiles;

    /**
     * Enabled export as templates
     */
    @Column(name = "is_enabled_export_as_templates", columnDefinition = "boolean default false")
    private Boolean enabledExportAsTemplates;

    /**
     * 所属 Data Facet 的 UID
     */
    @Column(name = "data_facet_uid")
    private Long dataFacetUid;
}
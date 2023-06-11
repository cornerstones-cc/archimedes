package cc.cornerstones.biz.datafacet.dto;

import cc.cornerstones.biz.export.share.constants.ExportCsvStrategyEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ExportBasicDto {
    /**
     * Enabled export csv
     */
    private Boolean enabledExportCsv;

    /**
     * If enabled export csv, what the export csv strategy is
     */
    private ExportCsvStrategyEnum exportCsvStrategy;

    /**
     * Enabled export excel
     */
    private Boolean enabledExportExcel;

    /**
     * Enabled export data and images
     */
    private Boolean enabledExportDataAndImages;

    /**
     * Enabled export data and files
     */
    private Boolean enabledExportDataAndFiles;

    /**
     * Enabled export as template
     */
    private Boolean enabledExportAsTemplates;

    /**
     * 所属 Data Facet 的 UID
     */
    private Long dataFacetUid;
}

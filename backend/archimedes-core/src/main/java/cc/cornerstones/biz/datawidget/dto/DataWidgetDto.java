package cc.cornerstones.biz.datawidget.dto;

import cc.cornerstones.almond.types.BaseDto;
import cc.cornerstones.biz.datawidget.share.constants.DataWidgetTypeEnum;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class DataWidgetDto extends BaseDto {
    private Long uid;

    private String name;

    private String objectName;

    private String description;

    /**
     * Type
     */
    private DataWidgetTypeEnum type;

    /**
     * Remark
     */
    private String remark;

    /**
     * Build characteristics
     */
    private JSONObject buildCharacteristics;

    /**
     * Serve characteristics
     */
    private Object serveCharacteristics;

    /**
     * 所属 Data Facet 的 UID
     */
    private Long dataFacetUid;
}

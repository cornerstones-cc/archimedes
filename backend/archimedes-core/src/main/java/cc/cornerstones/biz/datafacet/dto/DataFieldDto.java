package cc.cornerstones.biz.datafacet.dto;

import cc.cornerstones.almond.types.BaseDto;
import cc.cornerstones.biz.datafacet.share.constants.MeasurementRoleEnum;
import cc.cornerstones.biz.datafacet.share.constants.DataFieldTypeEnum;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class DataFieldDto extends BaseDto {

    /**
     * UID
     */
    private Long uid;

    /**
     * Name
     * <p>
     * A name is used to identify the object.
     */
    private String name;

    /**
     * Object Name
     * <p>
     * An object name is how the object is referenced programmatically.
     */
    private String objectName;

    /**
     * Label
     * <p>
     * An label is an additional name for better reading
     */
    private String label;

    /**
     * Description
     * <p>
     * A meaning description helps you remembers the differences between objects.
     */
    private String description;

    /**
     * Type
     */
    private DataFieldTypeEnum type;

    /**
     * Type extension
     *
     */
    private JSONObject typeExtension;

    /**
     * Measurement role
     */
    private MeasurementRoleEnum measurementRole;

    /**
     * Sequence
     *
     * 在所有字段中的序号（从0开始计数）
     */
    private Float sequence;

    /**
     * 所属 Data Facet 的 UID
     */
    private Long dataFacetUid;
}

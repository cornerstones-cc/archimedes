package cc.cornerstones.biz.datasource.dto;

import cc.cornerstones.almond.types.BaseDto;
import cc.cornerstones.biz.datasource.share.constants.DatabaseServerTypeEnum;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;

/**
 * Data Source
 *
 * @author bbottong
 *
 */
@Data
public class DataSourceDto extends BaseDto {
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
     * Description
     *
     * A meaning description helps you remembers the differences between objects.
     */
    private String description;

    /**
     * Type
     */
    private DatabaseServerTypeEnum type;

    /**
     * Connection Profile
     */
    private JSONObject connectionProfile;

    /**
     * 所属 Database Server 的 UID
     */
    private Long databaseServerUid;
}
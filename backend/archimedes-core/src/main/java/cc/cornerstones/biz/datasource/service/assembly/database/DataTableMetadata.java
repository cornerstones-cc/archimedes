package cc.cornerstones.biz.datasource.service.assembly.database;

import cc.cornerstones.biz.datasource.share.constants.DataTableTypeEnum;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.List;

@Data
public class DataTableMetadata {
    /**
     * Name
     * <p>
     * A name is how the object is referenced programmatically.
     * It must contain only lowercase letters, numbers, and underscores.
     * It must begin with a letter and be unique, and must not include spaces, end with an underscore,
     * or contain two consecutive underscores.
     */
    private String name;

    /**
     * Description
     * <p>
     * A meaning description helps you remembers the differences between objects.
     */
    private String description;

    /**
     * Type
     */
    private DataTableTypeEnum type;

    /**
     * Context Path
     */
    private List<String> contextPath;

    /**
     * Extension
     */
    private JSONObject extension;
}

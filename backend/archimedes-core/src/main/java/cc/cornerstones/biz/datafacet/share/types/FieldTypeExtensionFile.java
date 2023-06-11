package cc.cornerstones.biz.datafacet.share.types;

import cc.cornerstones.biz.share.constants.FileSourceSettingsModeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class FieldTypeExtensionFile {
    private FileSourceSettingsModeEnum settingsMode;

    /**
     * if mode = HTTP_RELATIVE_URL
     */
    private String prefixForHttpRelativeUrl;

    /**
     * if mode = FILE_RELATIVE_LOCAL_PATH
     */
    private String prefixForFileRelativeLocalPath;

    /**
     * if mode = DFS_FILE
     */
    private Long dfsServiceAgentUid;

    /**
     * May contain multiple images in one field
     */
    private Boolean mayContainMultipleItemsInOneField;

    /**
     * if mayContainMultipleItemsInOneField = true, the delimiter symbols may be
     */
    private String delimiter;
}

package cc.cornerstones.biz.share.types;

import cc.cornerstones.biz.datafacet.share.constants.DataFieldTypeEnum;
import lombok.Data;

@Data
public class PlainSelectionField {
    private String fieldName;
    private String fieldLabel;
    private DataFieldTypeEnum fieldType;
}

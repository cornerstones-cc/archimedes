package cc.cornerstones.biz.share.types;

import cc.cornerstones.biz.share.constants.SelectionFieldTypeEnum;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class SelectionField {
    private SelectionFieldTypeEnum type;
    private JSONObject content;
}

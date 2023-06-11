package cc.cornerstones.biz.share.types;

import cc.cornerstones.biz.share.constants.ExpressionTypeEnum;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class ExpressionSelectionField {
    private ExpressionTypeEnum type;
    private JSONObject content;
}

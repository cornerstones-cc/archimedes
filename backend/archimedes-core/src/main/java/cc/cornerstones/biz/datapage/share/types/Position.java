package cc.cornerstones.biz.datapage.share.types;

import cc.cornerstones.biz.datapage.share.constants.LayoutElementTypeEnum;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.List;

@Data
public class Position {
    private String id;
    private List<String> children;
    private List<String> parents;
    private LayoutElementTypeEnum type;
    private JSONObject meta;
}

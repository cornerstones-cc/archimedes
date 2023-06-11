package cc.cornerstones.biz.serve.dto;

import cc.cornerstones.almond.types.BaseDto;
import cc.cornerstones.biz.serve.share.constants.SessionStatusEnum;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class SessionDto extends BaseDto {

    /**
     * UID
     */
    private Long uid;

    /**
     * name
     */
    private String name;

    /**
     * Content
     */
    private JSONObject content;

    /**
     * Status
     */
    private SessionStatusEnum status;

    /**
     * Data widget uid
     */
    private Long dataWidgetUid;
}

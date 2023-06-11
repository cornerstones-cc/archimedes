package cc.cornerstones.biz.datadictionary.dto;

import cc.cornerstones.almond.types.BaseDto;
import cc.cornerstones.biz.datatable.share.constants.DictionaryBuildTypeEnum;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class DictionaryBuildDto extends BaseDto {
    /**
     * 是否启用该流程
     */
    private Boolean enabled;

    /**
     * 流程调度 CRON 表达式
     */
    private String cronExpression;

    /**
     * 构建类型
     */
    private DictionaryBuildTypeEnum type;

    /**
     * 构建逻辑
     */
    private JSONObject logic;

    /**
     * 所属字典类目的UID
     */
    private Long dictionaryCategoryUid;
}

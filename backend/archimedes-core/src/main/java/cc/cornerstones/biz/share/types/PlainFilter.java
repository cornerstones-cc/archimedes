package cc.cornerstones.biz.share.types;

import cc.cornerstones.almond.types.AbcTuple4;
import cc.cornerstones.biz.datafacet.share.constants.DataFieldTypeEnum;
import cc.cornerstones.biz.share.constants.FilteringTypeEnum;
import lombok.Data;

@Data
public class PlainFilter {
    /**
     * 字段名称，字段取值，字段过滤类型，字段类型
     */
    private AbcTuple4<String, String[], FilteringTypeEnum, DataFieldTypeEnum> content;
}

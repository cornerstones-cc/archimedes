package cc.cornerstones.biz.share.types;

import cc.cornerstones.almond.types.AbcTuple3;
import cc.cornerstones.almond.types.AbcTuple4;
import cc.cornerstones.biz.datafacet.share.constants.DataFieldTypeEnum;
import cc.cornerstones.biz.share.constants.FilteringTypeEnum;
import lombok.Data;

import java.util.List;

@Data
public class DataPermissionFilter {
    /**
     *
     * 第2级（最外层）的 list item 是 OR 关系；
     * 第1级的 list item 是 AND 关系；
     * 第1级的 每个 list item 是一个 3 元 tuple，包括：字段名称，字段取值，字段类型
     */
    private List<List<AbcTuple3<String, String, DataFieldTypeEnum>>> content;
}

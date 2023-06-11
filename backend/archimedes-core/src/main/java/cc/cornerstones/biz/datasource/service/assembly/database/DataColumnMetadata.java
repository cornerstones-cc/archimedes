package cc.cornerstones.biz.datasource.service.assembly.database;

import cc.cornerstones.biz.datasource.share.constants.DataColumnTypeEnum;
import lombok.Data;

@Data
public class DataColumnMetadata {
    /**
     * 字段名
     */
    private String name;

    /**
     * 字段说明
     */
    private String description;

    /**
     * 原始字段顺序
     */
    private Float ordinalPosition;

    /**
     * 程序认定的字段类型，跟不同数据库服务器类型的字段类型要有个对应关系
     */
    private DataColumnTypeEnum type;

    /**
     * 字段
     */
    private String length;

    /**
     * true --- 允许为 null，false --- 不允许为 null
     */
    private Boolean nullable;
}

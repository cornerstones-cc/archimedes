package cc.cornerstones.biz.share.constants;

/**
 * Filtering Type
 *
 * @author bbottong
 */
public enum FilteringTypeEnum {
    /**
     * 精确文本
     */
    EQUALS_TEXT,
    /**
     * 全模糊文本
     */
    CONTAINS_TEXT,
    /**
     * 左模糊文本
     */
    ENDS_WITH_TEXT,
    /**
     * 右模糊文本
     */
    BEGINS_WITH_TEXT,
    /**
     * 日期范围 (yyyy-MM-dd to yyyy-MM-dd)
     */
    DATE_RANGE,
    /**
     * 时间范围 (HH:mm:ss to HH:mm:ss)
     */
    TIME_RANGE,
    /**
     * 时间戳范围 (yyyy-MM-dd HH:mm:ss to yyyy-MM-dd HH:mm:ss)
     */
    DATETIME_RANGE,
    /**
     * 数字范围
     */
    NUMBER_RANGE,
    /**
     * 单选下拉列表
     */
    DROP_DOWN_LIST_SINGLE,
    /**
     * 多选下拉列表
     */
    DROP_DOWN_LIST_MULTIPLE,
    /**
     * 单选联想输入
     */
    ASSOCIATING_SINGLE,
    /**
     * 多选联想输入
     */
    ASSOCIATING_MULTIPLE,
    /**
     * 级联单选下拉列表
     */
    CASCADING_DROP_DOWN_LIST_SINGLE,
    /**
     * 级联多选下拉列表
     */
    CASCADING_DROP_DOWN_LIST_MULTIPLE,
    /**
     *
     */
    IS_NULL,
    /**
     *
     */
    IS_NOT_NULL;
}

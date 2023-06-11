package cc.cornerstones.biz.export.share.constants;

public enum ExportTaskStatusEnum {
    /**
     * 初始化中
     */
    INITIALIZING,
    /**
     * 已初始化完成，即已创建
     */
    CREATED,
    /**
     * 计数
     */
    COUNTING,
    /**
     * 查询
     */
    QUERYING,
    /**
     * 读数并写本地文件中
     */
    FETCHING,
    /**
     * 将本地文件转移到持久文件存储系统中
     */
    TRANSFERRING,
    /**
     * 已完成
     */
    FINISHED,
    /**
     * 失败中
     */
    FAILING,
    /**
     * 已失败
     */
    FAILED,
    /**
     * 取消中
     */
    CANCELLING,
    /**
     * 已取消
     */
    CANCELED;
}

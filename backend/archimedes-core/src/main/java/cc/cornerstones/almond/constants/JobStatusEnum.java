package cc.cornerstones.almond.constants;

/**
 * Job 状态
 *
 * 参考: https://nightlies.apache.org/flink/flink-docs-release-1.13/docs/internals/job_scheduling/
 *
 * @author bbottong
 */
public enum JobStatusEnum {
    /**
     * 初始化中
     */
    INITIALIZING,
    /**
     * 已初始化完成，即已创建，待运行: NEW, or RESTARTING ---(restarted job)--> CREATED
     */
    CREATED,
    /**
     * 进行中: CREATED ----schedule job---> RUNNING
     */
    RUNNING,
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
    CANCELED,
    /**
     * 已完成
     */
    FINISHED,
    /**
     * 重启中
     */
    RESTARTING,
    /**
     * 已暂停
     */
    SUSPENDED,
    /**
     * 调解中
     */
    RECONCILING;
}

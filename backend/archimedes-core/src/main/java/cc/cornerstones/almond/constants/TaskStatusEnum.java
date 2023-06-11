package cc.cornerstones.almond.constants;

/**
 * Task 状态
 *
 * 参考: https://nightlies.apache.org/flink/flink-docs-release-1.13/docs/internals/job_scheduling/
 *
 * @author bbottong
 */
public enum TaskStatusEnum {
    /**
     * 已创建
     */
    CREATED,
    /**
     * 调度中，CREATED ----schedule job (SCHEDULING) ---> RUNNING
     */
    SCHEDULING,
    /**
     * 进行中
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

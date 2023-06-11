package cc.cornerstones.archimedes.extensions.constants;

public enum DtsExecutorRouteStrategyEnum {
    /** 永远第一个实例 */
    FIRST,
    /** 永远最后一个实例 */
    LAST,
    /** 轮询 */
    ROUND,
    /** 随机 */
    RANDOM,
    /** 一致性hash，寻找一个实例，但是每个任务的实例不一样 */
    CONSISTENT_HASH,
    /** 最不经常使用 */
    LEAST_FREQUENTLY_USED,
    /** 最近最久未使用 */
    LEAST_RECENTLY_USED,
    /** 故障转移 */
    FAILOVER,
    /** 忙碌转移 */
    BUSYOVER,
    /** 分片广播 */
    SHARDING_BROADCAST;
}

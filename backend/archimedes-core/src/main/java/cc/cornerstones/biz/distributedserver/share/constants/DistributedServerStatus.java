package cc.cornerstones.biz.distributedserver.share.constants;

public enum DistributedServerStatus {
    /**
     * Ready to send/receive traffic
     */
    UP("Up"),
    /**
     * Do not send/receive traffic
     */
    DOWN("Down");

    final String symbol;

    DistributedServerStatus(String symbol) {
        this.symbol = symbol;
    }
}

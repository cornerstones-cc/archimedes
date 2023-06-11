package cc.cornerstones.biz.distributedjob.share.constants;

public enum JobExecutorRoutingAlgorithmEnum {
    /**
     * Round-robin (RR)
     */
    ROUND_ROBIN("Round-robin"),
    /**
     * Random
     */
    RANDOM("Random"),
    /**
     * All
     */
    BROADCAST("Broadcast");

    final String symbol;

    JobExecutorRoutingAlgorithmEnum(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}

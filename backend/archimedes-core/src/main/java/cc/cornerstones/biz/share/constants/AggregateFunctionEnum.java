package cc.cornerstones.biz.share.constants;

public enum AggregateFunctionEnum {
    SUM("Sum"),
    COUNT("Count"),
    AVERAGE("Average"),
    MAX("Max"),
    MIN("Min"),
    PRODUCT("Product"),
    COUNT_NUMBERS("Count Numbers"),
    STDDEV("StdDev"),
    STDEVP("StdDevp"),
    VAR("Var"),
    Varp("Varp");

    private final String symbol;

    AggregateFunctionEnum(String symbol) {
        this.symbol = symbol;
    }
}

package cc.cornerstones.almond.exceptions;

public class AbcCapacityLimitException extends AbcUndefinedException {
    public AbcCapacityLimitException(String errMessage) {
        super(ErrorCode.GENERAL_CAPACITY_LIMIT, errMessage);
    }
}

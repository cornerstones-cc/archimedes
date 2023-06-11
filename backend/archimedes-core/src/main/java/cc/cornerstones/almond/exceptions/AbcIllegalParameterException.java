package cc.cornerstones.almond.exceptions;

public class AbcIllegalParameterException extends AbcUndefinedException {
    public AbcIllegalParameterException(String errMessage) {
        super(ErrorCode.GENERAL_ILLEGAL_PARAMETER_ERROR, errMessage);
    }
}

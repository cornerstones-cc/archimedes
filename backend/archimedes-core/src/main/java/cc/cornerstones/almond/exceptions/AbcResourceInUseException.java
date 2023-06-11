package cc.cornerstones.almond.exceptions;

public class AbcResourceInUseException extends AbcUndefinedException {
    public AbcResourceInUseException(String errMessage) {
        super(ErrorCode.RESOURCE_IN_USE_ERROR, errMessage);
    }
}

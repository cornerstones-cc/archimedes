package cc.cornerstones.almond.exceptions;

public class AbcResourceDuplicateException extends AbcUndefinedException {
    public AbcResourceDuplicateException(String errMessage) {
        super(ErrorCode.RESOURCE_DUPLICATE_ERROR, errMessage);
    }
}

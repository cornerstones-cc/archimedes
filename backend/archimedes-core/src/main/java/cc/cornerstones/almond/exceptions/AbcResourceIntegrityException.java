package cc.cornerstones.almond.exceptions;

public class AbcResourceIntegrityException extends AbcUndefinedException {
    public AbcResourceIntegrityException(String errMessage) {
        super(ErrorCode.RESOURCE_INTEGRITY_ERROR, errMessage);
    }
}

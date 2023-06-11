package cc.cornerstones.almond.exceptions;

public class AbcResourceConflictException extends AbcUndefinedException {
    public AbcResourceConflictException(String errMessage) {
        super(ErrorCode.RESOURCE_CONFLICT_ERROR, errMessage);
    }
}

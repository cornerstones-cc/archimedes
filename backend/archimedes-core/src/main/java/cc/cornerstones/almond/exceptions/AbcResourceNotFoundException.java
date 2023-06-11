package cc.cornerstones.almond.exceptions;

public class AbcResourceNotFoundException extends AbcUndefinedException {
    public AbcResourceNotFoundException(String errMessage) {
        super(ErrorCode.RESOURCE_NOT_FOUND_ERROR, errMessage);
    }
}

package cc.cornerstones.almond.exceptions;

public class AbcResourcePersistenceException extends AbcUndefinedException {
    public AbcResourcePersistenceException(String message) {
        super(ErrorCode.RESOURCE_PERSISTENCE_ERROR, message);
    }
}

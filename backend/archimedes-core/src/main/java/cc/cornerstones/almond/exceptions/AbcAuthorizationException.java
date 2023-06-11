package cc.cornerstones.almond.exceptions;

public class AbcAuthorizationException extends AbcUndefinedException {
    public AbcAuthorizationException(String errMessage) {
        super(ErrorCode.AUTH_AUTHORIZATION_ERROR, errMessage);
    }
}

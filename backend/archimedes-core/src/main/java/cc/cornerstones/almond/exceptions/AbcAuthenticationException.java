package cc.cornerstones.almond.exceptions;

public class AbcAuthenticationException extends AbcUndefinedException {
    public AbcAuthenticationException(String errMessage) {
        super(ErrorCode.AUTH_AUTHENTICATION_ERROR, errMessage);
    }
}

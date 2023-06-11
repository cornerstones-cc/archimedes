package cc.cornerstones.almond.exceptions;

public class AbcUndefinedException extends RuntimeException {

    private String[] errorParams;

    private ErrorCode errorCode;

    public AbcUndefinedException(String errMessage) {
        super(errMessage);
        this.errorCode = ErrorCode.GENERAL_UNDEFINED_ERROR;
    }

    public AbcUndefinedException(ErrorCode errCode, String errMessage) {
        super(errMessage);
        this.errorCode = errCode;
    }

    public AbcUndefinedException(ErrorCode errCode, String errMessage, String[] errorParams) {
        super(errMessage);
        this.errorCode = errCode;
        this.errorParams = errorParams;
    }

    public AbcUndefinedException(String errMessage, Throwable e) {
        super(errMessage, e);
        this.errorCode = ErrorCode.GENERAL_UNDEFINED_ERROR;
    }

    public AbcUndefinedException(ErrorCode errCode, String errMessage, Throwable e) {
        super(errMessage, e);
        this.errorCode = errCode;
    }

    public String[] getErrorParams() {
        return errorParams;
    }

    public void setErrorParams(String[] errorParams) {
        this.errorParams = errorParams;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }
}

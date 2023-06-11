package cc.cornerstones.almond.types;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author zxiuwu
 * <p>
 * Response to caller
 */
public class Response<T> {
    @JsonProperty(value = "successful")
    private boolean successful;

    @JsonProperty(value = "err_code")
    private Integer errCode;

    @JsonProperty(value = "err_message")
    private String errMessage;

    private T object;

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public Integer getErrCode() {
        return errCode;
    }

    public void setErrCode(Integer errCode) {
        this.errCode = errCode;
    }

    public String getErrMessage() {
        return errMessage;
    }

    public void setErrMessage(String errMessage) {
        this.errMessage = errMessage;
    }

    public T getObject() {
        return object;
    }

    public void setObject(T object) {
        this.object = object;
    }

    @Override
    public String toString() {
        return "Response [successful=" + successful + ", errCode=" + errCode + ", errMessage=" + errMessage + "]";
    }

    public static Response buildFailure(Integer errCode, String errMessage) {
        Response response = new Response();
        response.setSuccessful(false);
        response.setErrCode(errCode);
        response.setErrMessage(errMessage);
        return response;
    }

    public static Response buildFailure() {
        Response response = new Response();
        response.setSuccessful(false);
        return response;
    }

    public static Response buildFailure(String message) {
        Response response = new Response();
        response.setSuccessful(false);
        response.setErrMessage(message);
        return response;
    }

    public static Response buildSuccess() {
        Response response = new Response();
        response.setSuccessful(true);
        return response;
    }

    public static <T> Response<T> buildSuccess(T o) {
        Response<T> response = new Response<>();
        response.setSuccessful(true);
        response.setObject(o);
        return response;
    }

}

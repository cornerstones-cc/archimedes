package cc.cornerstones.archimedes.extensions.usync.uc;


import java.util.List;

public class UcResponsePayloadDto {

    private boolean successful;

    private Integer errCode;

    private String errMessage;

    private String requestId;

    private List<ApiResponseUser> object;

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

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public List<ApiResponseUser> getObject() {
        return object;
    }

    public void setObject(List<ApiResponseUser> object) {
        this.object = object;
    }
}

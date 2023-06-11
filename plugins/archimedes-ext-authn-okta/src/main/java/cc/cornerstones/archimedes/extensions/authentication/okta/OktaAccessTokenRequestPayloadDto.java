package cc.cornerstones.archimedes.extensions.authentication.okta;

public class OktaAccessTokenRequestPayloadDto {
    private String redirectUri;
    private String code;

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}

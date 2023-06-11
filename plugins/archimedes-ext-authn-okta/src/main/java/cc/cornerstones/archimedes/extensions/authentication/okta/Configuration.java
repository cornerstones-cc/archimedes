package cc.cornerstones.archimedes.extensions.authentication.okta;

public class Configuration {
    /**
     * 申请的 client_id
     */
    private String clientId;

    /**
     * 申请的 client_secret
     */
    private String clientSecret;

    /**
     * 固定为 authorization_code
     */
    private String grantType = "authorization_code";

    /**
     * Token uri
     */
    private String tokenUri;

    /**
     * Userinfo uri
     */
    private String userinfoUri;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public String getTokenUri() {
        return tokenUri;
    }

    public void setTokenUri(String tokenUri) {
        this.tokenUri = tokenUri;
    }

    public String getUserinfoUri() {
        return userinfoUri;
    }

    public void setUserinfoUri(String userinfoUri) {
        this.userinfoUri = userinfoUri;
    }
}

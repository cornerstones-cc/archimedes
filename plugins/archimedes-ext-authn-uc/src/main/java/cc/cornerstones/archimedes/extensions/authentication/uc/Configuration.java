package cc.cornerstones.archimedes.extensions.authentication.uc;

public class Configuration {
    /**
     * Token uri
     */
    private String tokenUri;

    /**
     * Userinfo uri
     */
    private String userinfoUri;

    /**
     * Username
     */
    private String username;

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}

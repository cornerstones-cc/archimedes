package cc.cornerstones.archimedes.extensions.authentication.uc;

public class UcAccessTokenRequestPayloadDto {
    private String accounts;
    private String password;
    private String psSalt;

    public String getAccounts() {
        return accounts;
    }

    public void setAccounts(String accounts) {
        this.accounts = accounts;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPsSalt() {
        return psSalt;
    }

    public void setPsSalt(String psSalt) {
        this.psSalt = psSalt;
    }
}

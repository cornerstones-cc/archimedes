package cc.cornerstones.archimedes.extensions.authentication.okta;

public class OktaUserInfoResponsePayloadDto {
    /**
     * 显示名称，例如：He, James
     */
    private String name;

    /**
     * 电子邮件，例如：james.he@effem.com
     */
    private String preferredUsername;

    /**
     * 玛氏AD，例如：hexue
     */
    private String adusername;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPreferredUsername() {
        return preferredUsername;
    }

    public void setPreferredUsername(String preferredUsername) {
        this.preferredUsername = preferredUsername;
    }

    public String getAdusername() {
        return adusername;
    }

    public void setAdusername(String adusername) {
        this.adusername = adusername;
    }
}

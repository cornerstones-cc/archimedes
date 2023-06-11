package cc.cornerstones.archimedes.extensions.usync.uc;

public class ApiResponseUser {
    private String code;
    private String name;
    private String adNumber;
    private String email;
    // role code,role name
    // role code,role name;role code,role name;role code,role name
    private String roleInfo;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAdNumber() {
        return adNumber;
    }

    public void setAdNumber(String adNumber) {
        this.adNumber = adNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRoleInfo() {
        return roleInfo;
    }

    public void setRoleInfo(String roleInfo) {
        this.roleInfo = roleInfo;
    }
}

package cc.cornerstones.archimedes.extensions.usync.uc;

public class RoleMappingUnit {
    private String sourceRoleCode;
    private String sourceRoleName;
    private Long targetRoleUid;
    private String targetRoleName;

    public String getSourceRoleCode() {
        return sourceRoleCode;
    }

    public void setSourceRoleCode(String sourceRoleCode) {
        this.sourceRoleCode = sourceRoleCode;
    }

    public String getSourceRoleName() {
        return sourceRoleName;
    }

    public void setSourceRoleName(String sourceRoleName) {
        this.sourceRoleName = sourceRoleName;
    }

    public Long getTargetRoleUid() {
        return targetRoleUid;
    }

    public void setTargetRoleUid(Long targetRoleUid) {
        this.targetRoleUid = targetRoleUid;
    }

    public String getTargetRoleName() {
        return targetRoleName;
    }

    public void setTargetRoleName(String targetRoleName) {
        this.targetRoleName = targetRoleName;
    }
}

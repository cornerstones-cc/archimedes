package cc.cornerstones.archimedes.extensions.usync.uc;

public class AccountTypeMappingUnit {
    private String name;
    private Long targetAccountTypeUid;
    private String targetAccountTypeName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getTargetAccountTypeUid() {
        return targetAccountTypeUid;
    }

    public void setTargetAccountTypeUid(Long targetAccountTypeUid) {
        this.targetAccountTypeUid = targetAccountTypeUid;
    }

    public String getTargetAccountTypeName() {
        return targetAccountTypeName;
    }

    public void setTargetAccountTypeName(String targetAccountTypeName) {
        this.targetAccountTypeName = targetAccountTypeName;
    }
}

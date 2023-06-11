package cc.cornerstones.archimedes.extensions.usync.uc;

public class GroupMappingUnit {
    private String sourceGroupCode;
    private String sourceGroupName;
    private Long targetGroupUid;
    private String targetGroupName;

    public String getSourceGroupCode() {
        return sourceGroupCode;
    }

    public void setSourceGroupCode(String sourceGroupCode) {
        this.sourceGroupCode = sourceGroupCode;
    }

    public String getSourceGroupName() {
        return sourceGroupName;
    }

    public void setSourceGroupName(String sourceGroupName) {
        this.sourceGroupName = sourceGroupName;
    }

    public Long getTargetGroupUid() {
        return targetGroupUid;
    }

    public void setTargetGroupUid(Long targetGroupUid) {
        this.targetGroupUid = targetGroupUid;
    }

    public String getTargetGroupName() {
        return targetGroupName;
    }

    public void setTargetGroupName(String targetGroupName) {
        this.targetGroupName = targetGroupName;
    }
}

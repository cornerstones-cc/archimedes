package cc.cornerstones.archimedes.extensions.usync.uc;


import java.util.List;

public class Configuration {
    /**
     * User list uri
     */
    private String userListUri;

    private String username;

    private List<RoleMappingUnit> roleMappingUnits;

    private List<GroupMappingUnit> groupMappingUnits;

    private List<AccountTypeMappingUnit> accountTypeMappingUnits;

    private List<ExtendedPropertyMappingUnit> extendedPropertyMappingUnits;

    public String getUserListUri() {
        return userListUri;
    }

    public void setUserListUri(String userListUri) {
        this.userListUri = userListUri;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<RoleMappingUnit> getRoleMappingUnits() {
        return roleMappingUnits;
    }

    public void setRoleMappingUnits(List<RoleMappingUnit> roleMappingUnits) {
        this.roleMappingUnits = roleMappingUnits;
    }

    public List<GroupMappingUnit> getGroupMappingUnits() {
        return groupMappingUnits;
    }

    public void setGroupMappingUnits(List<GroupMappingUnit> groupMappingUnits) {
        this.groupMappingUnits = groupMappingUnits;
    }

    public List<AccountTypeMappingUnit> getAccountTypeMappingUnits() {
        return accountTypeMappingUnits;
    }

    public void setAccountTypeMappingUnits(List<AccountTypeMappingUnit> accountTypeMappingUnits) {
        this.accountTypeMappingUnits = accountTypeMappingUnits;
    }

    public List<ExtendedPropertyMappingUnit> getExtendedPropertyMappingUnits() {
        return extendedPropertyMappingUnits;
    }

    public void setExtendedPropertyMappingUnits(List<ExtendedPropertyMappingUnit> extendedPropertyMappingUnits) {
        this.extendedPropertyMappingUnits = extendedPropertyMappingUnits;
    }
}

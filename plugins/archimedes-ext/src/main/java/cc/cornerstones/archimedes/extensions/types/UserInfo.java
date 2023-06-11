package cc.cornerstones.archimedes.extensions.types;

import java.util.List;

public class UserInfo {
    private String displayName;

    private List<AccountInfo> accounts;

    private List<ExtendedPropertyInfo> extendedProperties;

    private List<Long> roleUidList;

    private List<Long> groupUidList;

    public static class AccountInfo {
        private Long accountTypeUid;
        private String accountName;

        public Long getAccountTypeUid() {
            return accountTypeUid;
        }

        public void setAccountTypeUid(Long accountTypeUid) {
            this.accountTypeUid = accountTypeUid;
        }

        public String getAccountName() {
            return accountName;
        }

        public void setAccountName(String accountName) {
            this.accountName = accountName;
        }
    }

    public static class ExtendedPropertyInfo {
        private Long extendedPropertyUid;
        private Object extendedPropertyValue;

        public Long getExtendedPropertyUid() {
            return extendedPropertyUid;
        }

        public void setExtendedPropertyUid(Long extendedPropertyUid) {
            this.extendedPropertyUid = extendedPropertyUid;
        }

        public Object getExtendedPropertyValue() {
            return extendedPropertyValue;
        }

        public void setExtendedPropertyValue(Object extendedPropertyValue) {
            this.extendedPropertyValue = extendedPropertyValue;
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<AccountInfo> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<AccountInfo> accounts) {
        this.accounts = accounts;
    }

    public List<ExtendedPropertyInfo> getExtendedProperties() {
        return extendedProperties;
    }

    public void setExtendedProperties(List<ExtendedPropertyInfo> extendedProperties) {
        this.extendedProperties = extendedProperties;
    }

    public List<Long> getRoleUidList() {
        return roleUidList;
    }

    public void setRoleUidList(List<Long> roleUidList) {
        this.roleUidList = roleUidList;
    }

    public List<Long> getGroupUidList() {
        return groupUidList;
    }

    public void setGroupUidList(List<Long> groupUidList) {
        this.groupUidList = groupUidList;
    }
}

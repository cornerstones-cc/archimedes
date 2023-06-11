package cc.cornerstones.archimedes.extensions.types;

import com.alibaba.fastjson.JSONObject;

public class SignedInfo {
    private Long accountTypeUid;
    private String accountName;
    private JSONObject userInfo;

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

    public JSONObject getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(JSONObject userInfo) {
        this.userInfo = userInfo;
    }
}

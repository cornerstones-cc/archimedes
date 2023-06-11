package cc.cornerstones.biz.administration.usermanagement.share.types;

import lombok.Data;

import java.util.List;

@Data
public class SimplePermissions {
    private List<Long> functionUidList;
    private List<Long> navigationMenuUidList;
}

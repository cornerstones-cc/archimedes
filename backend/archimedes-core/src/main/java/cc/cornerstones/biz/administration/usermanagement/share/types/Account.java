package cc.cornerstones.biz.administration.usermanagement.share.types;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Account {
    private Long accountTypeUid;
    private String accountTypeName;
    private String accountName;
    private LocalDateTime createdTimestamp;
}

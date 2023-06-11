package cc.cornerstones.biz.administration.usermanagement.dto;

import cc.cornerstones.biz.administration.usermanagement.share.types.Account;
import cc.cornerstones.biz.administration.usermanagement.share.types.ExtendedProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ReplaceUserDto {
    private Long uid;

    private Boolean enabled;

    @Pattern(regexp = "^[\\u4e00-\\u9fa5_a-zA-Z0-9\\s-]+$", message = "Only Chinese characters, or English letters, or numbers, or spaces, or underscores, or hyphens are allowed")
    @Size(min = 1, max = 64,
            message = "The display name cannot exceed 64 characters in length")
    private String displayName;

    private List<ExtendedProperty> extendedPropertyList;

    private List<Account> accountList;

    private List<Long> roleUidList;

    private List<Long> groupUidList;
}

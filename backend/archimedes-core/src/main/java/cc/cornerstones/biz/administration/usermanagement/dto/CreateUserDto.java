package cc.cornerstones.biz.administration.usermanagement.dto;

import cc.cornerstones.biz.administration.usermanagement.share.types.Account;
import cc.cornerstones.biz.administration.usermanagement.share.types.ExtendedProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class CreateUserDto {
    @NotBlank(message = "password is required")
    private String password;

    private Boolean enabled;

    @NotBlank(message = "display_name is required")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5_a-zA-Z0-9\\s]+", message = "name 必须由中文，或英文字母，或数字，或下划线组成")
    @Size(min = 1, max = 64,
            message = "The display_name cannot exceed 64 characters in length")
    private String displayName;

    private List<ExtendedProperty> extendedPropertyList;

    private List<Account> accountList;

    private List<Long> roleUidList;

    private List<Long> groupUidList;
}

package cc.cornerstones.biz.settings.dto;

import lombok.Data;

@Data
public class UpdateUserCredentialDto {
    private String oldPassword;
    private String newPassword;
}

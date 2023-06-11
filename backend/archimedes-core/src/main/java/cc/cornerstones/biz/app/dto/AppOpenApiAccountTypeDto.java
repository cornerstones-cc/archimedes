package cc.cornerstones.biz.app.dto;

import cc.cornerstones.almond.types.BaseDto;
import lombok.Data;

import java.util.List;

@Data
public class AppOpenApiAccountTypeDto extends BaseDto {
    /**
     * App's uid
     */
    private Long appUid;

    /**
     * App's account type uid
     */
    private List<Long> accountTypeUidList;

}

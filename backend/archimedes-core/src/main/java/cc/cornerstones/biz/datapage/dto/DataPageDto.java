package cc.cornerstones.biz.datapage.dto;

import cc.cornerstones.almond.types.BaseDto;
import lombok.Data;

@Data
public class DataPageDto extends BaseDto {
    private Long uid;

    private String name;

    private String objectName;

    private String description;

    /**
     * Remark
     */
    private String remark;
}

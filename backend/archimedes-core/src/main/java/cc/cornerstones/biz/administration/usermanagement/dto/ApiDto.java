package cc.cornerstones.biz.administration.usermanagement.dto;

import cc.cornerstones.almond.types.BaseDto;
import lombok.Data;

@Data
public class ApiDto extends BaseDto {

    /**
     * URI
     */
    private String uri;

    /**
     * Method
     */
    private String method;

    /**
     * Summary
     */
    private String summary;

    /**
     * Tag
     */
    private String tag;
}

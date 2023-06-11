package cc.cornerstones.biz.administration.usermanagement.share.types;


import io.swagger.v3.oas.annotations.enums.ParameterIn;
import lombok.Data;

import java.util.Map;

@Data
public class SwaggerParameterMetadata {
    private String name;

    private String label;

    private String description;

    /**
     * 是否必填参数
     */
    private Boolean required;

    /**
     * 参数顺序
     */
    private Integer ordinalPosition;

    /**
     * header/body/query
     */
    private ParameterIn in;

    /**
     * if in = body, type is a JSON
     */
    private SwaggerFieldTypeEnum type;

    /**
     * 参数的长度
     */
    private String length;

    /**
     * if in = body, 补充属性
     */
    private Map<String, Object> properties;
}

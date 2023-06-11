package cc.cornerstones.biz.administration.usermanagement.share.types;

import lombok.Data;

import java.util.Map;

@Data
public class SwaggerResponseMetadata {
    private Map<String, Object> properties;
    private SwaggerFieldTypeEnum type;
}

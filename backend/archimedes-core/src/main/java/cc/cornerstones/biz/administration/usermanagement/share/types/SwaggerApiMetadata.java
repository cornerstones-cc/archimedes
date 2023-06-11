package cc.cornerstones.biz.administration.usermanagement.share.types;

import lombok.Data;

@Data
public class SwaggerApiMetadata {
    private String summary;
    private String method;
    private String uri;
    private String tag;
}
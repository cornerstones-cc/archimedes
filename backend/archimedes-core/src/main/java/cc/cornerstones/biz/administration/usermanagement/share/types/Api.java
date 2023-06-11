package cc.cornerstones.biz.administration.usermanagement.share.types;

import lombok.Data;

@Data
public class Api {
    private String uri;
    private String method;
    private String summary;
    private String tag;
}

package cc.cornerstones.biz.administration.usermanagement.share.types;

import lombok.Data;

@Data
public class NavigationMenuEntityNode {
    private String uri;
    private String icon;

    /**
     * Component name
     */
    private String componentName;
}

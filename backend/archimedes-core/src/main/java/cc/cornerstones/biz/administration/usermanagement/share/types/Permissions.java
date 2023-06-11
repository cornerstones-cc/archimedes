package cc.cornerstones.biz.administration.usermanagement.share.types;

import lombok.Data;

import java.util.List;

@Data
public class Permissions {
    private List<Function> functionList;
    private List<NavigationMenu> navigationMenuList;
}

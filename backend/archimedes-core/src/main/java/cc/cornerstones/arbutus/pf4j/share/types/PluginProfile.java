package cc.cornerstones.arbutus.pf4j.share.types;

import lombok.Data;

@Data
public class PluginProfile {
    private String pluginId;
    private String pluginDescription;
    private String pluginClass;
    private String version;
    private String requires;
    private String provider;
    private String license;
}

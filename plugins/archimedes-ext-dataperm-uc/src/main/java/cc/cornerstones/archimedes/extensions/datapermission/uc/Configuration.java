package cc.cornerstones.archimedes.extensions.datapermission.uc;

public class Configuration {
    /**
     * Resource structure list uri
     */
    private String resourceStructureListUri;

    /**
     * public username to access resource structure list uri
     */
    private String username;

    /**
     * Resource content list uri
     */
    private String resourceContentListUri;

    public String getResourceStructureListUri() {
        return resourceStructureListUri;
    }

    public void setResourceStructureListUri(String resourceStructureListUri) {
        this.resourceStructureListUri = resourceStructureListUri;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getResourceContentListUri() {
        return resourceContentListUri;
    }

    public void setResourceContentListUri(String resourceContentListUri) {
        this.resourceContentListUri = resourceContentListUri;
    }
}

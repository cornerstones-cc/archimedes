package cc.cornerstones.archimedes.extensions.constants;

public enum DtsExecutorBlockStrategyEnum {
    SERIAL_EXECUTION("Serial execution"),
    DISCARD_LATER("Discard Later"),
    COVER_EARLY("Cover Early");

    private String title;

    private DtsExecutorBlockStrategyEnum(String title) {
        this.title = title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public static DtsExecutorBlockStrategyEnum match(String name, DtsExecutorBlockStrategyEnum defaultItem) {
        if (name != null) {
            for (DtsExecutorBlockStrategyEnum item : DtsExecutorBlockStrategyEnum.values()) {
                if (item.name().equals(name)) {
                    return item;
                }
            }
        }
        return defaultItem;
    }
}

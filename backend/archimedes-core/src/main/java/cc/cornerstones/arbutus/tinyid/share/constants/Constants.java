package cc.cornerstones.arbutus.tinyid.share.constants;

public class Constants {
    /**
     * 预加载下个号段的百分比
     */
    public static final int LOADING_PERCENT = 20;

    /**
     * 重试次数
     */
    public static final int RETRY = 10;

    private Constants() {

    }

    Integer IS_DELETED = 1;
    Integer NOT_DELETED = 0;

    Integer STATE_ENABLE = 1;
    Integer STATE_DISABLE = 0;
}

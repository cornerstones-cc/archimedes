package cc.cornerstones.almond.constants;

/**
 * @author bbottong
 */
public abstract class DatabaseConstants {
    public static final Integer CHAR_DEFAULT_LENGTH = 45;
    public static final Integer VARCHAR_DEFAULT_LENGTH = 45;
    public static final Integer UUID_DEFAULT_LENGTH = 36;
    public static final Integer ID_DEFAULT_LENGTH = 20;
    public static final String DECIMAL_DEFAULT_TYPE_ATTRIBUTES = "17,0";
    public static final Integer TEXT_MIN_LENGTH = 1000;

    /**
     * name, label, description 默认长度
     */
    public static final Integer DEFAULT_NAME_LENGTH = 100;
    public static final Integer DEFAULT_LABEL_LENGTH = 200;
    public static final Integer DEFAULT_DESCRIPTION_LENGTH = 255;

    public static final int DELETED_FLAG = 1;
    public static final int NOT_DELETED_FLAG = 0;

    public static final int MAXIMUM_FILE_NAME_LENGTH = 150;
}

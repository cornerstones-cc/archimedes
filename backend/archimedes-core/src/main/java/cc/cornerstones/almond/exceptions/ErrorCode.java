package cc.cornerstones.almond.exceptions;

public enum ErrorCode {
    /**
     * General
     */
    GENERAL_UNDEFINED_ERROR(10000),
    GENERAL_ILLEGAL_PARAMETER_ERROR(10001),
    GENERAL_IO_ERROR(10002),
    GENERAL_SQL_ERROR(10003),
    GENERAL_CAPACITY_LIMIT(10004),

    /**
     * Auth
     */
    AUTH_AUTHENTICATION_ERROR(10110),
    AUTH_APP_KEY_NOT_FOUND_ERROR(10111),
    AUTH_SIGNATURE_ERROR(10112),
    AUTH_GRANT_TYPE_ERROR(10113),
    AUTH_REFRESH_TOKEN_ERROR(10114),
    AUTH_ACCOUNT_NOT_FOUND_ERROR(10115),
    AUTH_AUTHORIZATION_ERROR(10116),

    /**
     * Resource
     */
    RESOURCE_NOT_FOUND_ERROR(10210),
    RESOURCE_DUPLICATE_ERROR(10211),
    RESOURCE_IN_USE_ERROR(10212),
    RESOURCE_CONFLICT_ERROR(10213),
    RESOURCE_INTEGRITY_ERROR(10214),
    RESOURCE_PERSISTENCE_ERROR(10215),
    RESOURCE_DEPENDENCY_ERROR(10216);

    private int code;

    ErrorCode(int code) {
        this.code = code;
    }

    public Integer getCode() {
        return this.code;
    }

    public String getCodeSymbol() {
        return this.name();
    }
}

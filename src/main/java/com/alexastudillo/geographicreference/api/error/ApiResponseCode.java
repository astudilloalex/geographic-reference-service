package com.alexastudillo.geographicreference.api.error;

/**
 * API response codes and their corresponding HTTP statuses.
 */
public enum ApiResponseCode {

    SUCCESSFUL(ApiErrorCode.SUCCESSFUL, 200),
    BAD_REQUEST(ApiErrorCode.BAD_REQUEST, 400),
    INVALID_UUID(ApiErrorCode.INVALID_UUID, 400),
    INVALID_STATUS(ApiErrorCode.INVALID_STATUS, 400),
    INVALID_FILTER_COMBINATION(ApiErrorCode.INVALID_FILTER_COMBINATION, 400),
    COUNTRY_NOT_FOUND(ApiErrorCode.COUNTRY_NOT_FOUND, 404),
    ADMINISTRATIVE_DIVISION_TYPE_NOT_FOUND(ApiErrorCode.ADMINISTRATIVE_DIVISION_TYPE_NOT_FOUND, 404),
    ADMINISTRATIVE_DIVISION_NOT_FOUND(ApiErrorCode.ADMINISTRATIVE_DIVISION_NOT_FOUND, 404),
    NOT_FOUND(ApiErrorCode.NOT_FOUND, 404),
    METHOD_NOT_ALLOWED(ApiErrorCode.METHOD_NOT_ALLOWED, 405),
    SERVER_ERROR(ApiErrorCode.SERVER_ERROR, 500);

    private final String code;
    private final int status;

    ApiResponseCode(final String code, final int status) {
        this.code = code;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public int status() {
        return status;
    }

    public static ApiResponseCode fromHttpStatus(final int status) {
        return switch (status) {
            case 400 -> BAD_REQUEST;
            case 404 -> NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            default -> SERVER_ERROR;
        };
    }
}

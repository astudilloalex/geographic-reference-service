package com.alexastudillo.geographicreference.api.error;

/**
 * Stable response code values exposed by the HTTP API.
 */
public final class ApiErrorCode {

    public static final String SUCCESSFUL = "successful";
    public static final String BAD_REQUEST = "bad-request";
    public static final String INVALID_UUID = "invalid-uuid";
    public static final String INVALID_STATUS = "invalid-status";
    public static final String INVALID_FILTER_COMBINATION = "invalid-filter-combination";
    public static final String COUNTRY_NOT_FOUND = "country-not-found";
    public static final String ADMINISTRATIVE_DIVISION_TYPE_NOT_FOUND =
            "administrative-division-type-not-found";
    public static final String ADMINISTRATIVE_DIVISION_NOT_FOUND = "administrative-division-not-found";
    public static final String NOT_FOUND = "not-found";
    public static final String METHOD_NOT_ALLOWED = "method-not-allowed";
    public static final String SERVER_ERROR = "server-error";

    private ApiErrorCode() {
    }
}

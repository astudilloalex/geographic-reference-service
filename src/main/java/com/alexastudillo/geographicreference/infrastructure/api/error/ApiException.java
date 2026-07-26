package com.alexastudillo.geographicreference.infrastructure.api.error;

import java.util.Objects;

/**
 * Expected API-layer failure represented by a stable response code.
 */
public class ApiException extends RuntimeException {

    private final ApiResponseCode responseCode;

    public ApiException(final ApiResponseCode responseCode) {
        this.responseCode = Objects.requireNonNull(responseCode, "ApiResponseCode must not be null");
    }

    public ApiResponseCode responseCode() {
        return responseCode;
    }
}

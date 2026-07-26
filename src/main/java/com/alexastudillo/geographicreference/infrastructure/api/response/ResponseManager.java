package com.alexastudillo.geographicreference.infrastructure.api.response;

import com.alexastudillo.geographicreference.infrastructure.api.error.ApiResponseCode;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Creates consistent API response envelopes.
 */
@ApplicationScoped
public class ResponseManager {

    public <T> ApiResponse<T> success(final T data) {
        return customResponse(ApiResponseCode.SUCCESSFUL, data);
    }

    public ApiResponse<Void> success() {
        return customResponse(ApiResponseCode.SUCCESSFUL, null);
    }

    public ApiResponse<Void> error(final ApiResponseCode responseCode) {
        return customResponse(responseCode, null);
    }

    public <T> ApiResponse<T> customResponse(final ApiResponseCode responseCode, final T data) {
        return new ApiResponse<>(responseCode.status(), responseCode.code(), data);
    }
}

package com.alexastudillo.geographicreference.api.error;

import com.alexastudillo.geographicreference.api.response.ApiResponse;
import com.alexastudillo.geographicreference.api.response.ResponseManager;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ApiErrorComponentsTest {

    @Test
    void shouldExposeStableCodesAndStatuses() {
        assertThat(ApiResponseCode.SUCCESSFUL.code()).isEqualTo(ApiErrorCode.SUCCESSFUL);
        assertThat(ApiResponseCode.INVALID_UUID.status()).isEqualTo(400);
        assertThat(ApiResponseCode.INVALID_STATUS.status()).isEqualTo(400);
        assertThat(ApiResponseCode.INVALID_FILTER_COMBINATION.status()).isEqualTo(400);
        assertThat(ApiResponseCode.COUNTRY_NOT_FOUND.status()).isEqualTo(404);
        assertThat(ApiResponseCode.ADMINISTRATIVE_DIVISION_TYPE_NOT_FOUND.status()).isEqualTo(404);
        assertThat(ApiResponseCode.ADMINISTRATIVE_DIVISION_NOT_FOUND.status()).isEqualTo(404);
        assertThat(ApiResponseCode.SERVER_ERROR.status()).isEqualTo(500);

        assertThat(ApiResponseCode.fromHttpStatus(400)).isEqualTo(ApiResponseCode.BAD_REQUEST);
        assertThat(ApiResponseCode.fromHttpStatus(404)).isEqualTo(ApiResponseCode.NOT_FOUND);
        assertThat(ApiResponseCode.fromHttpStatus(405)).isEqualTo(ApiResponseCode.METHOD_NOT_ALLOWED);
        assertThat(ApiResponseCode.fromHttpStatus(418)).isEqualTo(ApiResponseCode.SERVER_ERROR);
    }

    @Test
    void apiExceptionShouldRequireAndExposeResponseCode() {
        final ApiException exception = new ApiException(ApiResponseCode.COUNTRY_NOT_FOUND);

        assertThat(exception.responseCode()).isEqualTo(ApiResponseCode.COUNTRY_NOT_FOUND);
        assertThatNullPointerException().isThrownBy(() -> new ApiException(null));
    }

    @Test
    void handlerShouldMapExpectedAndFrameworkExceptionsReactively() {
        final ExceptionHandler handler = handler();

        assertResponse(
                handler.handleApiException(new ApiException(ApiResponseCode.COUNTRY_NOT_FOUND))
                        .await().indefinitely(),
                404,
                "country-not-found"
        );
        assertResponse(
                handler.handleWebApplicationException(new BadRequestException()).await().indefinitely(),
                400,
                "bad-request"
        );
        assertResponse(
                handler.handleWebApplicationException(new NotFoundException()).await().indefinitely(),
                404,
                "not-found"
        );
        assertResponse(
                handler.handleWebApplicationException(new NotAllowedException("GET")).await().indefinitely(),
                405,
                "method-not-allowed"
        );
    }

    @Test
    void handlerShouldHideUnexpectedFailures() {
        assertResponse(
                handler().handleUnexpectedException(new IllegalStateException("database details"))
                        .await().indefinitely(),
                500,
                "server-error"
        );
    }

    private ExceptionHandler handler() {
        return new ExceptionHandler(new ResponseManager());
    }

    private void assertResponse(
            final RestResponse<ApiResponse<Void>> response,
            final int status,
            final String code
    ) {
        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getEntity().getStatus()).isEqualTo(status);
        assertThat(response.getEntity().getCode()).isEqualTo(code);
        assertThat(response.getEntity().getData()).isNull();
    }
}

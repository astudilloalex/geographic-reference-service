package com.alexastudillo.geographicreference.api.error;

import com.alexastudillo.geographicreference.api.response.ApiResponse;
import com.alexastudillo.geographicreference.api.response.ResponseManager;
import com.alexastudillo.geographicreference.domain.utils.LogUtil;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Global reactive exception mapping for the public API.
 */
@Provider
@Slf4j
public class ExceptionHandler {

    private static final String HANDLER = "API EXCEPTION HANDLER";

    private final ResponseManager responseManager;

    @Inject
    public ExceptionHandler(ResponseManager responseManager) {
        this.responseManager = responseManager;
    }

    @ServerExceptionMapper(ApiException.class)
    public Uni<RestResponse<ApiResponse<Void>>> handleApiException(final ApiException exception) {
        log.warn(LogUtil.log(
                HANDLER,
                "Handled API exception: code=%s, status=%s",
                exception.responseCode().code(),
                exception.responseCode().status()));
        return Uni.createFrom().item(errorResponse(exception.responseCode()));
    }

    @ServerExceptionMapper(WebApplicationException.class)
    public Uni<RestResponse<ApiResponse<Void>>> handleWebApplicationException(
            final WebApplicationException exception) {
        final ApiResponseCode responseCode =
                ApiResponseCode.fromHttpStatus(exception.getResponse().getStatus());
        log.warn(LogUtil.log(
                HANDLER,
                "Handled HTTP exception: code=%s, status=%s",
                responseCode.code(),
                responseCode.status()));
        return Uni.createFrom().item(errorResponse(responseCode));
    }

    @ServerExceptionMapper(Throwable.class)
    public Uni<RestResponse<ApiResponse<Void>>> handleUnexpectedException(final Throwable throwable) {
        log.error(LogUtil.log(HANDLER, "Unhandled API exception"), throwable);
        return Uni.createFrom().item(errorResponse(ApiResponseCode.SERVER_ERROR));
    }

    private RestResponse<ApiResponse<Void>> errorResponse(final ApiResponseCode responseCode) {
        return RestResponse.status(
                Response.Status.fromStatusCode(responseCode.status()),
                responseManager.error(responseCode));
    }
}

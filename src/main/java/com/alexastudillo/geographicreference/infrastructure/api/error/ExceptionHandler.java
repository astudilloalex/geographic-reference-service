package com.alexastudillo.geographicreference.infrastructure.api.error;

import com.alexastudillo.geographicreference.infrastructure.api.response.ApiResponse;
import com.alexastudillo.geographicreference.infrastructure.api.response.ResponseManager;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Global reactive exception mapping for the public API.
 */
@Provider
public class ExceptionHandler {

    private static final Logger LOG = Logger.getLogger(ExceptionHandler.class);

    private final ResponseManager responseManager;

    @Inject
    public ExceptionHandler(ResponseManager responseManager) {
        this.responseManager = responseManager;
    }

    @ServerExceptionMapper(ApiException.class)
    public Uni<RestResponse<ApiResponse<Void>>> handleApiException(final ApiException exception) {
        return Uni.createFrom().item(errorResponse(exception.responseCode()));
    }

    @ServerExceptionMapper(WebApplicationException.class)
    public Uni<RestResponse<ApiResponse<Void>>> handleWebApplicationException(
            final WebApplicationException exception) {
        return Uni.createFrom().item(errorResponse(
                ApiResponseCode.fromHttpStatus(exception.getResponse().getStatus())));
    }

    @ServerExceptionMapper(Throwable.class)
    public Uni<RestResponse<ApiResponse<Void>>> handleUnexpectedException(final Throwable throwable) {
        LOG.error("Unhandled API exception", throwable);
        return Uni.createFrom().item(errorResponse(ApiResponseCode.SERVER_ERROR));
    }

    private RestResponse<ApiResponse<Void>> errorResponse(final ApiResponseCode responseCode) {
        return RestResponse.status(
                Response.Status.fromStatusCode(responseCode.status()),
                responseManager.error(responseCode));
    }
}

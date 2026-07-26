package com.alexastudillo.geographicreference.api.logging;

import com.alexastudillo.geographicreference.api.error.ApiException;
import com.alexastudillo.geographicreference.api.error.ApiResponseCode;
import com.alexastudillo.geographicreference.domain.utils.LogUtil;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * Validates request tracing headers and exposes them through the logging MDC.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
@Slf4j
public class MDCRequestFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String PROCESS_ID_HEADER = "process-id";
    public static final String USER_ID_HEADER = "user-id";
    public static final String COMPANY_ID_HEADER = "company-id";

    static final String PROCESS_ID_MDC_KEY = "processId";
    static final String USER_ID_MDC_KEY = "userId";
    static final String COMPANY_ID_MDC_KEY = "companyId";

    private static final String FILTER = "MDC REQUEST FILTER";
    private static final String PROCESS_ID_PROPERTY = MDCRequestFilter.class.getName() + ".processId";
    private static final String START_TIME_PROPERTY = MDCRequestFilter.class.getName() + ".startTime";
    private static final int MAX_USER_ID_LENGTH = 128;

    @Override
    public void filter(final ContainerRequestContext requestContext) {
        if (isManagementRequest(requestContext)) {
            return;
        }

        clearOwnedMdcValues();
        final String processId = resolveProcessId(
                requestContext.getHeaderString(PROCESS_ID_HEADER));
        requestContext.setProperty(PROCESS_ID_PROPERTY, processId);
        requestContext.setProperty(START_TIME_PROPERTY, System.nanoTime());
        MDC.put(PROCESS_ID_MDC_KEY, processId);

        final String userId = requireUserId(requestContext.getHeaderString(USER_ID_HEADER));
        final String companyId = optionalUuid(
                requestContext.getHeaderString(COMPANY_ID_HEADER),
                COMPANY_ID_HEADER);
        MDC.put(USER_ID_MDC_KEY, userId);
        if (companyId != null) {
            MDC.put(COMPANY_ID_MDC_KEY, companyId);
        }

        log.info(LogUtil.log(
                FILTER,
                "Starting request: method=%s, path=%s",
                requestContext.getMethod(),
                requestPath(requestContext)));
    }

    @Override
    public void filter(
            final ContainerRequestContext requestContext,
            final ContainerResponseContext responseContext) {
        if (isManagementRequest(requestContext)) {
            return;
        }

        try {
            final Object processId = requestContext.getProperty(PROCESS_ID_PROPERTY);
            if (processId != null) {
                responseContext.getHeaders().putSingle(PROCESS_ID_HEADER, processId);
            }

            log.info(LogUtil.log(
                    FILTER,
                    "Completed request: method=%s, path=%s, status=%s, durationMs=%s",
                    requestContext.getMethod(),
                    requestPath(requestContext),
                    responseContext.getStatus(),
                    elapsedMilliseconds(requestContext)));
        } finally {
            clearOwnedMdcValues();
        }
    }

    private String resolveProcessId(final String rawProcessId) {
        if (rawProcessId == null || rawProcessId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return requiredUuid(rawProcessId, PROCESS_ID_HEADER);
    }

    private String requireUserId(final String rawUserId) {
        if (rawUserId == null || rawUserId.isBlank()) {
            throw invalidHeader(USER_ID_HEADER, "must not be blank");
        }

        final String userId = rawUserId.trim();
        if (userId.length() > MAX_USER_ID_LENGTH) {
            throw invalidHeader(USER_ID_HEADER, "must not exceed 128 characters");
        }
        if (userId.chars().anyMatch(Character::isISOControl)) {
            throw invalidHeader(USER_ID_HEADER, "must not contain control characters");
        }
        return userId;
    }

    private String optionalUuid(final String rawValue, final String headerName) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        return requiredUuid(rawValue, headerName);
    }

    private String requiredUuid(final String rawValue, final String headerName) {
        final String value = rawValue.trim();
        try {
            final UUID uuid = UUID.fromString(value);
            if (!uuid.toString().equalsIgnoreCase(value)) {
                throw invalidHeader(headerName, "must be a canonical UUID");
            }
            return uuid.toString();
        } catch (IllegalArgumentException _) {
            throw invalidHeader(headerName, "must be a UUID");
        }
    }

    private ApiException invalidHeader(final String headerName, final String reason) {
        log.warn(LogUtil.log(
                FILTER,
                "Invalid request header: header=%s, reason=%s",
                headerName,
                reason));
        return new ApiException(ApiResponseCode.BAD_REQUEST);
    }

    private long elapsedMilliseconds(final ContainerRequestContext requestContext) {
        final Object startTime = requestContext.getProperty(START_TIME_PROPERTY);
        if (!(startTime instanceof Long startedAt)) {
            return 0;
        }
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private boolean isManagementRequest(final ContainerRequestContext requestContext) {
        final String path = requestPath(requestContext);
        return path.equals("/q") || path.startsWith("/q/");
    }

    private String requestPath(final ContainerRequestContext requestContext) {
        final String path = requestContext.getUriInfo().getPath();
        if (path == null || path.isEmpty()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private void clearOwnedMdcValues() {
        MDC.remove(PROCESS_ID_MDC_KEY);
        MDC.remove(USER_ID_MDC_KEY);
        MDC.remove(COMPANY_ID_MDC_KEY);
    }
}

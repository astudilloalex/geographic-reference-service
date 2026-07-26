package com.alexastudillo.geographicreference.api.logging;

import com.alexastudillo.geographicreference.api.error.ApiException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MDCRequestFilterTest {

    private static final String PROCESS_ID = "8d9989fa-034f-4457-bd8c-bf03e63d10a4";
    private static final String COMPANY_ID = "f7358ef2-b707-4c80-90a7-41f202c2628f";

    private final MDCRequestFilter filter = new MDCRequestFilter();

    @AfterEach
    void clearMdc() {
        MDC.remove(MDCRequestFilter.PROCESS_ID_MDC_KEY);
        MDC.remove(MDCRequestFilter.USER_ID_MDC_KEY);
        MDC.remove(MDCRequestFilter.COMPANY_ID_MDC_KEY);
    }

    @Test
    void shouldGenerateProcessIdAndClearOwnedMdcValuesAfterResponse() {
        final RequestFixture fixture = request("/api/v1/countries", null, "  api-user  ", null);
        final ContainerResponseContext response = response(200);

        filter.filter(fixture.context());

        final String generatedProcessId = MDC.get(MDCRequestFilter.PROCESS_ID_MDC_KEY);
        assertThat(UUID.fromString(generatedProcessId)).hasToString(generatedProcessId);
        assertThat(MDC.get(MDCRequestFilter.USER_ID_MDC_KEY)).isEqualTo("api-user");
        assertThat(MDC.get(MDCRequestFilter.COMPANY_ID_MDC_KEY)).isNull();

        filter.filter(fixture.context(), response);

        assertThat(response.getHeaders().getFirst(MDCRequestFilter.PROCESS_ID_HEADER))
                .isEqualTo(generatedProcessId);
        assertThat(MDC.get(MDCRequestFilter.PROCESS_ID_MDC_KEY)).isNull();
        assertThat(MDC.get(MDCRequestFilter.USER_ID_MDC_KEY)).isNull();
        assertThat(MDC.get(MDCRequestFilter.COMPANY_ID_MDC_KEY)).isNull();
    }

    @Test
    void shouldPreserveCanonicalProcessAndOptionalCompanyIds() {
        final RequestFixture fixture = request(
                "api/v1/countries",
                " " + PROCESS_ID.toUpperCase() + " ",
                UUID.randomUUID().toString(),
                " " + COMPANY_ID.toUpperCase() + " ");

        filter.filter(fixture.context());

        assertThat(MDC.get(MDCRequestFilter.PROCESS_ID_MDC_KEY)).isEqualTo(PROCESS_ID);
        assertThat(MDC.get(MDCRequestFilter.COMPANY_ID_MDC_KEY)).isEqualTo(COMPANY_ID);
    }

    @Test
    void shouldSkipManagementRequests() {
        final RequestFixture root = request("/q", null, null, null);
        final RequestFixture health = request("/q/health", null, null, null);
        final ContainerResponseContext response = response(200);

        filter.filter(root.context());
        filter.filter(health.context(), response);

        verify(root.context(), never()).setProperty(anyString(), any());
        assertThat(response.getHeaders()).isEmpty();
    }

    @Test
    void shouldRejectInvalidProcessIdentifiers() {
        assertBadRequest(request("/", "not-a-uuid", "user", null));
        assertBadRequest(request("/", "1-1-1-1-1", "user", null));
    }

    @Test
    void shouldRejectMissingOversizedAndUnsafeUserIdentifiers() {
        assertBadRequest(request("/", PROCESS_ID, null, null));
        assertBadRequest(request("/", PROCESS_ID, "   ", null));
        assertBadRequest(request("/", PROCESS_ID, "a".repeat(129), null));
        assertBadRequest(request("/", PROCESS_ID, "user\nforged", null));
    }

    @Test
    void shouldAcceptMaximumUserLengthAndBlankCompanyId() {
        final RequestFixture fixture = request("/", PROCESS_ID, "a".repeat(128), "   ");

        filter.filter(fixture.context());

        assertThat(MDC.get(MDCRequestFilter.USER_ID_MDC_KEY)).hasSize(128);
        assertThat(MDC.get(MDCRequestFilter.COMPANY_ID_MDC_KEY)).isNull();
    }

    @Test
    void shouldRejectInvalidCompanyIdentifiers() {
        assertBadRequest(request("/", PROCESS_ID, "user", "company"));
        assertBadRequest(request("/", PROCESS_ID, "user", "1-1-1-1-1"));
    }

    @Test
    void responseWithoutRequestStateShouldRemainSafeAndClearStaleMdc() {
        final RequestFixture fixture = request("", null, null, null);
        final ContainerResponseContext response = response(500);
        MDC.put(MDCRequestFilter.PROCESS_ID_MDC_KEY, PROCESS_ID);

        filter.filter(fixture.context(), response);

        assertThat(response.getHeaders()).isEmpty();
        assertThat(MDC.get(MDCRequestFilter.PROCESS_ID_MDC_KEY)).isNull();
    }

    private void assertBadRequest(final RequestFixture fixture) {
        final ContainerRequestContext context = fixture.context();
        assertThatThrownBy(() -> filter.filter(context))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).responseCode().status())
                .isEqualTo(400);
    }

    private RequestFixture request(
            final String path,
            final String processId,
            final String userId,
            final String companyId
    ) {
        final ContainerRequestContext request = mock(ContainerRequestContext.class);
        final UriInfo uriInfo = mock(UriInfo.class);
        final Map<String, Object> properties = new HashMap<>();
        when(request.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn(path);
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeaderString(MDCRequestFilter.PROCESS_ID_HEADER)).thenReturn(processId);
        when(request.getHeaderString(MDCRequestFilter.USER_ID_HEADER)).thenReturn(userId);
        when(request.getHeaderString(MDCRequestFilter.COMPANY_ID_HEADER)).thenReturn(companyId);
        doAnswer(invocation -> properties.put(
                invocation.getArgument(0),
                invocation.getArgument(1)))
                .when(request).setProperty(anyString(), any());
        when(request.getProperty(anyString()))
                .thenAnswer(invocation -> properties.get(invocation.getArgument(0)));
        return new RequestFixture(request);
    }

    private ContainerResponseContext response(final int status) {
        final ContainerResponseContext response = mock(ContainerResponseContext.class);
        when(response.getStatus()).thenReturn(status);
        when(response.getHeaders()).thenReturn(new MultivaluedHashMap<>());
        return response;
    }

    private record RequestFixture(ContainerRequestContext context) {
    }
}

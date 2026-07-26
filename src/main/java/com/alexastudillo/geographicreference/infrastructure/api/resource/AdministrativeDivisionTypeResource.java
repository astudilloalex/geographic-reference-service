package com.alexastudillo.geographicreference.infrastructure.api.resource;

import com.alexastudillo.geographicreference.application.dto.CountryResponse;
import com.alexastudillo.geographicreference.application.port.input.GetAdministrativeDivisionTypeQueryPort;
import com.alexastudillo.geographicreference.application.port.input.GetCountryQueryPort;
import com.alexastudillo.geographicreference.infrastructure.api.dto.AdministrativeDivisionTypeApiResponse;
import com.alexastudillo.geographicreference.infrastructure.api.error.ApiException;
import com.alexastudillo.geographicreference.infrastructure.api.error.ApiResponseCode;
import com.alexastudillo.geographicreference.infrastructure.api.mapper.AdministrativeDivisionTypeRestMapper;
import com.alexastudillo.geographicreference.infrastructure.api.response.ApiResponse;
import com.alexastudillo.geographicreference.infrastructure.api.response.ResponseManager;
import com.alexastudillo.geographicreference.infrastructure.api.validation.ApiRequestValidator;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;

import java.util.List;
import java.util.UUID;

/**
 * Reactive HTTP adapter for administrative division type queries.
 */
@Path("/api/v1/countries/{countryId}/division-types")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Administrative division types", description = "Country-specific hierarchy level definitions")
@org.eclipse.microprofile.openapi.annotations.responses.APIResponse(responseCode = "400", description = "Invalid identifier or status", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
@org.eclipse.microprofile.openapi.annotations.responses.APIResponse(responseCode = "404", description = "Country or administrative division type not found", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
@org.eclipse.microprofile.openapi.annotations.responses.APIResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
public class AdministrativeDivisionTypeResource {

        private final GetAdministrativeDivisionTypeQueryPort queryPort;
        private final GetCountryQueryPort countryQueryPort;
        private final AdministrativeDivisionTypeRestMapper mapper;
        private final ResponseManager responseManager;
        private final ApiRequestValidator validator;

        public AdministrativeDivisionTypeResource(
                        final GetAdministrativeDivisionTypeQueryPort queryPort,
                        final GetCountryQueryPort countryQueryPort,
                        final AdministrativeDivisionTypeRestMapper mapper,
                        final ResponseManager responseManager,
                        final ApiRequestValidator validator) {
                this.queryPort = queryPort;
                this.countryQueryPort = countryQueryPort;
                this.mapper = mapper;
                this.responseManager = responseManager;
                this.validator = validator;
        }

        @GET
        @Operation(summary = "List administrative division types")
        public Uni<RestResponse<ApiResponse<List<AdministrativeDivisionTypeApiResponse>>>> list(
                        @PathParam("countryId") final String countryId,
                        @Parameter(description = "DRAFT, ACTIVE, DEPRECATED, or RETIRED") @QueryParam("status") final String status) {
                final UUID parsedCountryId = validator.uuid(countryId);
                final String parsedStatus = status == null ? null : validator.status(status);
                return existingCountry(parsedCountryId)
                                .chain(ignored -> parsedStatus == null
                                                ? queryPort.listByCountryId(parsedCountryId)
                                                : queryPort.listByCountryIdAndStatus(parsedCountryId, parsedStatus))
                                .map(items -> items.stream().map(mapper::toApiResponse).toList())
                                .map(responseManager::success)
                                .map(RestResponse::ok);
        }

        @GET
        @Path("/by-code/{code}")
        @Operation(summary = "Get administrative division type by country and code")
        public Uni<RestResponse<ApiResponse<AdministrativeDivisionTypeApiResponse>>> findByCode(
                        @PathParam("countryId") final String countryId,
                        @PathParam("code") final String code) {
                final UUID parsedCountryId = validator.uuid(countryId);
                final String parsedCode = validator.code(code);
                return existingCountry(parsedCountryId)
                                .chain(ignored -> queryPort.findByCountryIdAndCode(parsedCountryId, parsedCode))
                                .onItem().ifNull().failWith(this::divisionTypeNotFound)
                                .map(mapper::toApiResponse)
                                .map(responseManager::success)
                                .map(RestResponse::ok);
        }

        @GET
        @Path("/{divisionTypeId}")
        @Operation(summary = "Get administrative division type by ID")
        public Uni<RestResponse<ApiResponse<AdministrativeDivisionTypeApiResponse>>> findById(
                        @PathParam("countryId") final String countryId,
                        @PathParam("divisionTypeId") final String divisionTypeId) {
                final UUID parsedCountryId = validator.uuid(countryId);
                final UUID parsedDivisionTypeId = validator.uuid(divisionTypeId);
                return existingCountry(parsedCountryId)
                                .chain(ignored -> queryPort.findById(parsedDivisionTypeId))
                                .onItem().ifNull().failWith(this::divisionTypeNotFound)
                                .invoke(item -> requireSameCountry(parsedCountryId, item.countryId()))
                                .map(mapper::toApiResponse)
                                .map(responseManager::success)
                                .map(RestResponse::ok);
        }

        private Uni<CountryResponse> existingCountry(final UUID countryId) {
                return countryQueryPort.findById(countryId)
                                .onItem().ifNull().failWith(
                                                () -> new ApiException(ApiResponseCode.COUNTRY_NOT_FOUND));
        }

        private void requireSameCountry(final UUID requestedCountryId, final UUID actualCountryId) {
                if (!requestedCountryId.equals(actualCountryId)) {
                        throw divisionTypeNotFound();
                }
        }

        private ApiException divisionTypeNotFound() {
                return new ApiException(ApiResponseCode.ADMINISTRATIVE_DIVISION_TYPE_NOT_FOUND);
        }
}

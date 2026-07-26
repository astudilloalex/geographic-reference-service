package com.alexastudillo.geographicreference.infrastructure.api.resource;

import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionResponse;
import com.alexastudillo.geographicreference.application.dto.CountryResponse;
import com.alexastudillo.geographicreference.application.port.input.GetAdministrativeDivisionQueryPort;
import com.alexastudillo.geographicreference.application.port.input.GetCountryQueryPort;
import com.alexastudillo.geographicreference.infrastructure.api.dto.AdministrativeDivisionApiResponse;
import com.alexastudillo.geographicreference.infrastructure.api.dto.AdministrativeDivisionIdentifierApiResponse;
import com.alexastudillo.geographicreference.infrastructure.api.dto.AdministrativeDivisionNameApiResponse;
import com.alexastudillo.geographicreference.infrastructure.api.error.ApiException;
import com.alexastudillo.geographicreference.infrastructure.api.error.ApiResponseCode;
import com.alexastudillo.geographicreference.infrastructure.api.mapper.AdministrativeDivisionRestMapper;
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
 * Reactive HTTP adapter for administrative division queries.
 */
@Path("/api/v1/countries/{countryId}/administrative-divisions")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Administrative divisions", description = "Country administrative hierarchy queries")
@org.eclipse.microprofile.openapi.annotations.responses.APIResponse(
        responseCode = "400",
        description = "Invalid identifier, status, or filter combination",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))
)
@org.eclipse.microprofile.openapi.annotations.responses.APIResponse(
        responseCode = "404",
        description = "Country or administrative division not found",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))
)
@org.eclipse.microprofile.openapi.annotations.responses.APIResponse(
        responseCode = "500",
        description = "Unexpected server error",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))
)
public class AdministrativeDivisionResource {

    private final GetAdministrativeDivisionQueryPort queryPort;
    private final GetCountryQueryPort countryQueryPort;
    private final AdministrativeDivisionRestMapper mapper;
    private final ResponseManager responseManager;
    private final ApiRequestValidator validator;

    public AdministrativeDivisionResource(
            final GetAdministrativeDivisionQueryPort queryPort,
            final GetCountryQueryPort countryQueryPort,
            final AdministrativeDivisionRestMapper mapper,
            final ResponseManager responseManager,
            final ApiRequestValidator validator
    ) {
        this.queryPort = queryPort;
        this.countryQueryPort = countryQueryPort;
        this.mapper = mapper;
        this.responseManager = responseManager;
        this.validator = validator;
    }

    @GET
    @Operation(summary = "List administrative divisions", description = "Supports country, parent, root, or type/status queries")
    public Uni<RestResponse<ApiResponse<List<AdministrativeDivisionApiResponse>>>> list(
            @PathParam("countryId") final String countryId,
            @Parameter(description = "Parent division UUID; cannot be combined with other filters")
            @QueryParam("parentId") final String parentId,
            @Parameter(description = "Use true to return root divisions; cannot be combined with other filters")
            @QueryParam("root") final String root,
            @Parameter(description = "Division type UUID; must be combined with status")
            @QueryParam("typeId") final String typeId,
            @Parameter(description = "Lifecycle status; must be combined with typeId")
            @QueryParam("status") final String status
    ) {
        final UUID parsedCountryId = validator.uuid(countryId);
        final Uni<List<AdministrativeDivisionResponse>> result =
                selectListQuery(parsedCountryId, parentId, root, typeId, status);
        return existingCountry(parsedCountryId)
                .chain(ignored -> result)
                .map(items -> items.stream().map(mapper::toApiResponse).toList())
                .map(responseManager::success)
                .map(RestResponse::ok);
    }

    @GET
    @Path("/by-canonical-code/{canonicalCode}")
    @Operation(summary = "Get administrative division by canonical code")
    public Uni<RestResponse<ApiResponse<AdministrativeDivisionApiResponse>>> findByCanonicalCode(
            @PathParam("countryId") final String countryId,
            @PathParam("canonicalCode") final String canonicalCode
    ) {
        final UUID parsedCountryId = validator.uuid(countryId);
        final String parsedCanonicalCode = validator.code(canonicalCode);
        return existingCountry(parsedCountryId)
                .chain(ignored -> queryPort.findByCanonicalCode(
                        parsedCountryId,
                        parsedCanonicalCode
                ))
                .onItem().ifNull().failWith(this::divisionNotFound)
                .map(mapper::toApiResponse)
                .map(responseManager::success)
                .map(RestResponse::ok);
    }

    @GET
    @Path("/{divisionId}")
    @Operation(summary = "Get administrative division by ID")
    public Uni<RestResponse<ApiResponse<AdministrativeDivisionApiResponse>>> findById(
            @PathParam("countryId") final String countryId,
            @PathParam("divisionId") final String divisionId
    ) {
        final UUID parsedCountryId = validator.uuid(countryId);
        return existingDivision(parsedCountryId, validator.uuid(divisionId))
                .map(mapper::toApiResponse)
                .map(responseManager::success)
                .map(RestResponse::ok);
    }

    @GET
    @Path("/{divisionId}/identifiers")
    @Operation(summary = "List identifiers for an administrative division")
    public Uni<RestResponse<ApiResponse<List<AdministrativeDivisionIdentifierApiResponse>>>> findIdentifiers(
            @PathParam("countryId") final String countryId,
            @PathParam("divisionId") final String divisionId
    ) {
        final UUID parsedCountryId = validator.uuid(countryId);
        final UUID parsedDivisionId = validator.uuid(divisionId);
        return existingDivision(parsedCountryId, parsedDivisionId)
                .chain(ignored -> queryPort.findIdentifiersByDivisionId(parsedCountryId, parsedDivisionId))
                .map(items -> items.stream().map(mapper::toIdentifierApiResponse).toList())
                .map(responseManager::success)
                .map(RestResponse::ok);
    }

    @GET
    @Path("/{divisionId}/names")
    @Operation(summary = "List localized names for an administrative division")
    public Uni<RestResponse<ApiResponse<List<AdministrativeDivisionNameApiResponse>>>> findNames(
            @PathParam("countryId") final String countryId,
            @PathParam("divisionId") final String divisionId
    ) {
        final UUID parsedCountryId = validator.uuid(countryId);
        final UUID parsedDivisionId = validator.uuid(divisionId);
        return existingDivision(parsedCountryId, parsedDivisionId)
                .chain(ignored -> queryPort.findNamesByDivisionId(parsedCountryId, parsedDivisionId))
                .map(items -> items.stream().map(mapper::toNameApiResponse).toList())
                .map(responseManager::success)
                .map(RestResponse::ok);
    }

    private Uni<List<AdministrativeDivisionResponse>> selectListQuery(
            final UUID countryId,
            final String parentId,
            final String root,
            final String typeId,
            final String status
    ) {
        if (parentId == null && root == null && typeId == null && status == null) {
            return queryPort.listByCountryId(countryId);
        }
        if (parentId != null && root == null && typeId == null && status == null) {
            return queryPort.listByParentId(countryId, validator.uuid(parentId));
        }
        if (parentId == null && root != null && typeId == null && status == null) {
            validator.root(root);
            return queryPort.listByParentId(countryId, null);
        }
        if (parentId == null && root == null && typeId != null && status != null) {
            return queryPort.listByTypeAndStatus(
                    countryId,
                    validator.uuid(typeId),
                    validator.status(status)
            );
        }
        throw validator.invalidFilters();
    }

    private Uni<CountryResponse> existingCountry(final UUID countryId) {
        return countryQueryPort.findById(countryId)
                .onItem().ifNull().failWith(
                        () -> new ApiException(ApiResponseCode.COUNTRY_NOT_FOUND)
                );
    }

    private Uni<AdministrativeDivisionResponse> existingDivision(
            final UUID countryId,
            final UUID divisionId
    ) {
        return existingCountry(countryId)
                .chain(ignored -> queryPort.findById(divisionId))
                .onItem().ifNull().failWith(this::divisionNotFound)
                .invoke(item -> {
                    if (!countryId.equals(item.countryId())) {
                        throw divisionNotFound();
                    }
                });
    }

    private ApiException divisionNotFound() {
        return new ApiException(ApiResponseCode.ADMINISTRATIVE_DIVISION_NOT_FOUND);
    }
}

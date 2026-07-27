package com.alexastudillo.geographicreference.api.resource;

import com.alexastudillo.geographicreference.application.dto.CountryResponse;
import com.alexastudillo.geographicreference.application.port.input.GetCountryQueryPort;
import com.alexastudillo.geographicreference.api.dto.CountryApiResponse;
import com.alexastudillo.geographicreference.api.dto.CountryNameLookupApiResponse;
import com.alexastudillo.geographicreference.api.dto.CountryNameApiResponse;
import com.alexastudillo.geographicreference.api.error.ApiException;
import com.alexastudillo.geographicreference.api.error.ApiResponseCode;
import com.alexastudillo.geographicreference.api.mapper.CountryRestMapper;
import com.alexastudillo.geographicreference.api.openapi.OpenApiResponses;
import com.alexastudillo.geographicreference.api.response.ApiResponse;
import com.alexastudillo.geographicreference.api.response.ResponseManager;
import com.alexastudillo.geographicreference.api.validation.ApiRequestValidator;
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
 * Reactive HTTP adapter for country queries.
 */
@Path("/api/v1/countries")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Countries", description = "Country and localized country-name queries")
@org.eclipse.microprofile.openapi.annotations.responses.APIResponse(responseCode = "400", description = "Invalid country identifier, code, or status", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
@org.eclipse.microprofile.openapi.annotations.responses.APIResponse(responseCode = "404", description = "Country not found", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
@org.eclipse.microprofile.openapi.annotations.responses.APIResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
public class CountryResource {

    private final GetCountryQueryPort queryPort;
    private final CountryRestMapper mapper;
    private final ResponseManager responseManager;
    private final ApiRequestValidator validator;

    public CountryResource(
            final GetCountryQueryPort queryPort,
            final CountryRestMapper mapper,
            final ResponseManager responseManager,
            final ApiRequestValidator validator) {
        this.queryPort = queryPort;
        this.mapper = mapper;
        this.responseManager = responseManager;
        this.validator = validator;
    }

    @GET
    @Operation(summary = "List countries", description = "Optionally filters countries by lifecycle status")
    @org.eclipse.microprofile.openapi.annotations.responses.APIResponse(
            responseCode = "200",
            description = "Countries returned successfully",
            content = @Content(schema = @Schema(implementation = OpenApiResponses.CountryListResponse.class))
    )
    public Uni<RestResponse<ApiResponse<List<CountryApiResponse>>>> list(
            @Parameter(description = "DRAFT, ACTIVE, DEPRECATED, or RETIRED") @QueryParam("status") final String status) {
        final Uni<List<CountryResponse>> result = status == null
                ? queryPort.listAll()
                : queryPort.listByStatus(validator.status(status));

        return result
                .map(items -> items.stream().map(mapper::toApiResponse).toList())
                .map(responseManager::success)
                .map(RestResponse::ok);
    }

    @GET
    @Path("/{countryId}")
    @Operation(summary = "Get country by ID")
    @org.eclipse.microprofile.openapi.annotations.responses.APIResponse(
            responseCode = "200",
            description = "Country returned successfully",
            content = @Content(schema = @Schema(implementation = OpenApiResponses.CountryResponse.class))
    )
    public Uni<RestResponse<ApiResponse<CountryApiResponse>>> findById(
            @PathParam("countryId") final String countryId) {
        return existingCountry(validator.uuid(countryId))
                .map(mapper::toApiResponse)
                .map(responseManager::success)
                .map(RestResponse::ok);
    }

    @GET
    @Path("/by-alpha2/{alpha2Code}")
    @Operation(summary = "Get country by ISO alpha-2 code")
    @org.eclipse.microprofile.openapi.annotations.responses.APIResponse(
            responseCode = "200",
            description = "Country returned successfully",
            content = @Content(schema = @Schema(implementation = OpenApiResponses.CountryResponse.class))
    )
    public Uni<RestResponse<ApiResponse<CountryApiResponse>>> findByAlpha2Code(
            @PathParam("alpha2Code") final String alpha2Code) {
        return requiredCountry(queryPort.findByAlpha2Code(validator.alpha2Code(alpha2Code)))
                .map(mapper::toApiResponse)
                .map(responseManager::success)
                .map(RestResponse::ok);
    }

    @GET
    @Path("/by-alpha3/{alpha3Code}")
    @Operation(summary = "Get country by ISO alpha-3 code")
    @org.eclipse.microprofile.openapi.annotations.responses.APIResponse(
            responseCode = "200",
            description = "Country returned successfully",
            content = @Content(schema = @Schema(implementation = OpenApiResponses.CountryResponse.class))
    )
    public Uni<RestResponse<ApiResponse<CountryApiResponse>>> findByAlpha3Code(
            @PathParam("alpha3Code") final String alpha3Code) {
        return requiredCountry(queryPort.findByAlpha3Code(validator.alpha3Code(alpha3Code)))
                .map(mapper::toApiResponse)
                .map(responseManager::success)
                .map(RestResponse::ok);
    }

    @GET
    @Path("/by-numeric/{numericCode}")
    @Operation(summary = "Get country by ISO numeric code")
    @org.eclipse.microprofile.openapi.annotations.responses.APIResponse(
            responseCode = "200",
            description = "Country returned successfully",
            content = @Content(schema = @Schema(implementation = OpenApiResponses.CountryResponse.class))
    )
    public Uni<RestResponse<ApiResponse<CountryApiResponse>>> findByNumericCode(
            @PathParam("numericCode") final String numericCode) {
        return requiredCountry(queryPort.findByNumericCode(validator.numericCode(numericCode)))
                .map(mapper::toApiResponse)
                .map(responseManager::success)
                .map(RestResponse::ok);
    }

    @GET
    @Path("/{countryId}/names")
    @Operation(summary = "List localized names for a country")
    @org.eclipse.microprofile.openapi.annotations.responses.APIResponse(
            responseCode = "200",
            description = "Country names returned successfully",
            content = @Content(schema = @Schema(implementation = OpenApiResponses.CountryNameListResponse.class))
    )
    public Uni<RestResponse<ApiResponse<List<CountryNameApiResponse>>>> findNames(
            @PathParam("countryId") final String countryId) {
        final UUID id = validator.uuid(countryId);
        return existingCountry(id)
                .chain(ignored -> queryPort.findNamesByCountryId(id))
                .map(items -> items.stream().map(mapper::toNameApiResponse).toList())
                .map(responseManager::success)
                .map(RestResponse::ok);
    }

    @GET
    @Path("/names")
    @Operation(
            summary = "List localized country names",
            description = "Returns country names filtered by ISO code type, geographic name type, and BCP 47 language tag"
    )
    @org.eclipse.microprofile.openapi.annotations.responses.APIResponse(
            responseCode = "200",
            description = "Localized country names returned successfully",
            content = @Content(schema = @Schema(implementation = OpenApiResponses.CountryNameLookupListResponse.class))
    )
    public Uni<RestResponse<ApiResponse<List<CountryNameLookupApiResponse>>>> findNames(
            @Parameter(
                    description = "ISO code returned for each country: ALPHA2, ALPHA3, or NUMERIC",
                    required = true
            )
            @QueryParam("codeType") final String codeType,
            @Parameter(
                    description = "OFFICIAL, COMMON, SHORT, ALTERNATIVE, or HISTORICAL",
                    required = true
            )
            @QueryParam("nameType") final String nameType,
            @Parameter(
                    description = "BCP 47 language tag, for example es, en, or es-EC",
                    required = true
            )
            @QueryParam("languageTag") final String languageTag
    ) {
        return queryPort.findNames(
                        validator.countryCodeType(codeType),
                        validator.nameType(nameType),
                        validator.languageTag(languageTag))
                .map(items -> items.stream().map(mapper::toNameLookupApiResponse).toList())
                .map(responseManager::success)
                .map(RestResponse::ok);
    }

    private Uni<CountryResponse> existingCountry(final UUID countryId) {
        return requiredCountry(queryPort.findById(countryId));
    }

    private Uni<CountryResponse> requiredCountry(final Uni<CountryResponse> result) {
        return result.onItem().ifNull().failWith(
                () -> new ApiException(ApiResponseCode.COUNTRY_NOT_FOUND));
    }
}

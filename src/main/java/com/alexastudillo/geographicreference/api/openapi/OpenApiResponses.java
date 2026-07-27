package com.alexastudillo.geographicreference.api.openapi;

import com.alexastudillo.geographicreference.api.dto.AdministrativeDivisionApiResponse;
import com.alexastudillo.geographicreference.api.dto.AdministrativeDivisionIdentifierApiResponse;
import com.alexastudillo.geographicreference.api.dto.AdministrativeDivisionNameApiResponse;
import com.alexastudillo.geographicreference.api.dto.AdministrativeDivisionTypeApiResponse;
import com.alexastudillo.geographicreference.api.dto.CountryApiResponse;
import com.alexastudillo.geographicreference.api.dto.CountryNameApiResponse;
import com.alexastudillo.geographicreference.api.dto.CountryNameLookupApiResponse;
import com.alexastudillo.geographicreference.api.response.ApiResponse;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

/**
 * Concrete response-envelope types used only to preserve generic payload information in OpenAPI.
 */
public final class OpenApiResponses {

    private OpenApiResponses() {
    }

    @Schema(name = "CountryListResponse")
    public static final class CountryListResponse extends ApiResponse<List<CountryApiResponse>> {
    }

    @Schema(name = "CountryResponse")
    public static final class CountryResponse extends ApiResponse<CountryApiResponse> {
    }

    @Schema(name = "CountryNameListResponse")
    public static final class CountryNameListResponse extends ApiResponse<List<CountryNameApiResponse>> {
    }

    @Schema(name = "CountryNameLookupListResponse")
    public static final class CountryNameLookupListResponse extends ApiResponse<List<CountryNameLookupApiResponse>> {
    }

    @Schema(name = "AdministrativeDivisionTypeListResponse")
    public static final class AdministrativeDivisionTypeListResponse
            extends ApiResponse<List<AdministrativeDivisionTypeApiResponse>> {
    }

    @Schema(name = "AdministrativeDivisionTypeResponse")
    public static final class AdministrativeDivisionTypeResponse
            extends ApiResponse<AdministrativeDivisionTypeApiResponse> {
    }

    @Schema(name = "AdministrativeDivisionListResponse")
    public static final class AdministrativeDivisionListResponse
            extends ApiResponse<List<AdministrativeDivisionApiResponse>> {
    }

    @Schema(name = "AdministrativeDivisionResponse")
    public static final class AdministrativeDivisionResponse
            extends ApiResponse<AdministrativeDivisionApiResponse> {
    }

    @Schema(name = "AdministrativeDivisionIdentifierListResponse")
    public static final class AdministrativeDivisionIdentifierListResponse
            extends ApiResponse<List<AdministrativeDivisionIdentifierApiResponse>> {
    }

    @Schema(name = "AdministrativeDivisionNameListResponse")
    public static final class AdministrativeDivisionNameListResponse
            extends ApiResponse<List<AdministrativeDivisionNameApiResponse>> {
    }
}

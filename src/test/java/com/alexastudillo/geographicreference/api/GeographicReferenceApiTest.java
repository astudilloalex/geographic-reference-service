package com.alexastudillo.geographicreference.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.filter.Filter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.alexastudillo.geographicreference.api.logging.MDCRequestFilter.COMPANY_ID_HEADER;
import static com.alexastudillo.geographicreference.api.logging.MDCRequestFilter.PROCESS_ID_HEADER;
import static com.alexastudillo.geographicreference.api.logging.MDCRequestFilter.USER_ID_HEADER;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.matchesPattern;

@QuarkusTest
class GeographicReferenceApiTest {

    private static final String ECUADOR_ID = "00000000-0000-7000-8000-000000000218";
    private static final String COLOMBIA_ID = "00000000-0000-7000-8000-000000000170";
    private static final String PROVINCE_TYPE_ID = "20000000-0000-7000-8000-000000000001";
    private static final String COLOMBIA_TYPE_ID = "20000000-0000-7000-8000-000000000003";
    private static final String PICHINCHA_ID = "30000000-0000-7000-8000-000000000002";
    private static final String QUITO_ID = "30000000-0000-7000-8000-000000000003";
    private static final String UNKNOWN_ID = "90000000-0000-7000-8000-000000000099";
    private static final String PROCESS_ID = "61c55f47-e889-4a34-b61d-07bb060ab496";
    private static final String USER_ID = "geographic-reference-api-test";

    private static List<Filter> originalFilters;

    @BeforeAll
    static void configureRequestLoggingHeaders() {
        originalFilters = List.copyOf(RestAssured.filters());
        final List<Filter> filters = new ArrayList<>(originalFilters);
        filters.add((request, response, context) -> {
            if (!request.getHeaders().hasHeaderWithName(PROCESS_ID_HEADER)) {
                request.header(PROCESS_ID_HEADER, PROCESS_ID);
            }
            if (!request.getHeaders().hasHeaderWithName(USER_ID_HEADER)) {
                request.header(USER_ID_HEADER, USER_ID);
            }
            return context.next(request, response);
        });
        RestAssured.replaceFiltersWith(filters);
    }

    @AfterAll
    static void restoreFilters() {
        RestAssured.replaceFiltersWith(originalFilters);
    }

    @Test
    void shouldListAndFilterCountriesWithStandardEnvelope() {
        given()
                .when().get("/api/v1/countries")
                .then()
                .statusCode(200)
                .header(PROCESS_ID_HEADER, equalTo(PROCESS_ID))
                .body("status", equalTo(200))
                .body("code", equalTo("successful"))
                .body("data.alpha2Code", contains("CO", "EC"))
                .body("$", not(hasKey("nextCursor")))
                .body("$", not(hasKey("totalElements")));

        given()
                .queryParam("status", " active ")
                .when().get("/api/v1/countries")
                .then()
                .statusCode(200)
                .body("data", hasSize(1))
                .body("data[0].alpha3Code", equalTo("ECU"));
    }

    @Test
    void shouldFindCountriesByEverySupportedIdentifier() {
        given()
                .when().get("/api/v1/countries/{id}", ECUADOR_ID)
                .then()
                .statusCode(200)
                .body("data.id", equalTo(ECUADOR_ID))
                .body("data.defaultName", equalTo("Ecuador"))
                .body("data.version", equalTo(2));

        given()
                .when().get("/api/v1/countries/by-alpha2/ec")
                .then()
                .statusCode(200)
                .body("data.alpha2Code", equalTo("EC"));

        given()
                .when().get("/api/v1/countries/by-alpha3/ecu")
                .then()
                .statusCode(200)
                .body("data.alpha3Code", equalTo("ECU"));

        given()
                .when().get("/api/v1/countries/by-numeric/218")
                .then()
                .statusCode(200)
                .body("data.numericCode", equalTo("218"));
    }

    @Test
    void shouldListCountryNames() {
        given()
                .when().get("/api/v1/countries/{id}/names", ECUADOR_ID)
                .then()
                .statusCode(200)
                .body("data", hasSize(2))
                .body("data.languageTag", contains("en", "es"))
                .body("data[1].name", equalTo("Ecuador"));
    }

    @Test
    void shouldListLocalizedCountryNamesUsingTheSelectedCodeType() {
        given()
                .queryParam("codeType", "alpha2")
                .queryParam("nameType", "common")
                .queryParam("languageTag", "ES")
                .when().get("/api/v1/countries/names")
                .then()
                .statusCode(200)
                .body("data", hasSize(1))
                .body("data[0].codeType", equalTo("ALPHA2"))
                .body("data[0].code", equalTo("EC"))
                .body("data[0].languageTag", equalTo("es"))
                .body("data[0].nameType", equalTo("COMMON"))
                .body("data[0].name", equalTo("Ecuador"))
                .body("data[0].preferred", equalTo(true));

        given()
                .queryParam("codeType", "NUMERIC")
                .queryParam("nameType", "COMMON")
                .queryParam("languageTag", "es")
                .when().get("/api/v1/countries/names")
                .then()
                .statusCode(200)
                .body("data[0].code", equalTo("218"));
    }

    @Test
    void shouldRejectInvalidCountryParametersAndReportMissingCountries() {
        assertError("/api/v1/countries/not-a-uuid", 400, "invalid-uuid");
        assertError("/api/v1/countries/by-alpha2/E", 400, "bad-request");
        assertError("/api/v1/countries/by-alpha3/E1U", 400, "bad-request");
        assertError("/api/v1/countries/by-numeric/2A8", 400, "bad-request");
        assertError("/api/v1/countries?status=unknown", 400, "invalid-status");
        assertError("/api/v1/countries/names?nameType=COMMON&languageTag=es", 400, "bad-request");
        assertError(
                "/api/v1/countries/names?codeType=UUID&nameType=COMMON&languageTag=es",
                400,
                "bad-request");
        assertError(
                "/api/v1/countries/names?codeType=ALPHA2&nameType=CANONICAL&languageTag=es",
                400,
                "bad-request");
        assertError(
                "/api/v1/countries/names?codeType=ALPHA2&nameType=COMMON&languageTag=not_a_tag",
                400,
                "bad-request");
        assertError("/api/v1/countries/" + UNKNOWN_ID, 404, "country-not-found");
        assertError("/api/v1/countries/" + UNKNOWN_ID + "/names", 404, "country-not-found");
    }

    @Test
    void shouldQueryAdministrativeDivisionTypes() {
        final String basePath = "/api/v1/countries/" + ECUADOR_ID + "/division-types";

        given()
                .when().get(basePath)
                .then()
                .statusCode(200)
                .body("data", hasSize(2))
                .body("data.code", contains("PROVINCE", "CANTON"));

        given()
                .queryParam("status", "ACTIVE")
                .when().get(basePath)
                .then()
                .statusCode(200)
                .body("data", hasSize(2));

        given()
                .when().get(basePath + "/by-code/province")
                .then()
                .statusCode(200)
                .body("data.id", equalTo(PROVINCE_TYPE_ID))
                .body("data.hierarchyLevel", equalTo(1));

        given()
                .when().get(basePath + "/" + PROVINCE_TYPE_ID)
                .then()
                .statusCode(200)
                .body("data.code", equalTo("PROVINCE"));
    }

    @Test
    void shouldValidateAdministrativeDivisionTypeContext() {
        final String basePath = "/api/v1/countries/" + ECUADOR_ID + "/division-types";

        assertError(
                "/api/v1/countries/" + UNKNOWN_ID + "/division-types",
                404,
                "country-not-found"
        );
        assertError(
                "/api/v1/countries/" + UNKNOWN_ID + "/division-types/" + PROVINCE_TYPE_ID,
                404,
                "country-not-found"
        );
        assertError(basePath + "/by-code/unknown", 404, "administrative-division-type-not-found");
        assertError(basePath + "/" + UNKNOWN_ID, 404, "administrative-division-type-not-found");
        assertError(basePath + "/" + COLOMBIA_TYPE_ID, 404, "administrative-division-type-not-found");
        assertError(basePath + "?status=unknown", 400, "invalid-status");
    }

    @Test
    void shouldListAdministrativeDivisionsWithEverySupportedFilter() {
        final String basePath = "/api/v1/countries/" + ECUADOR_ID + "/administrative-divisions";

        given()
                .when().get(basePath)
                .then()
                .statusCode(200)
                .body("data", hasSize(3))
                .body("data.canonicalCode", contains("EC-G", "EC-P", "EC-P-Q"));

        given()
                .queryParam("root", true)
                .when().get(basePath)
                .then()
                .statusCode(200)
                .body("data", hasSize(2))
                .body("data.canonicalCode", contains("EC-G", "EC-P"));

        given()
                .queryParam("parentId", PICHINCHA_ID)
                .when().get(basePath)
                .then()
                .statusCode(200)
                .body("data", hasSize(1))
                .body("data[0].canonicalCode", equalTo("EC-P-Q"));

        given()
                .queryParam("typeId", PROVINCE_TYPE_ID)
                .queryParam("status", "active")
                .when().get(basePath)
                .then()
                .statusCode(200)
                .body("data", hasSize(2));
    }

    @Test
    void shouldFindAdministrativeDivisionAndItsDetails() {
        final String basePath = "/api/v1/countries/" + ECUADOR_ID + "/administrative-divisions";

        given()
                .when().get(basePath + "/by-canonical-code/ec-p")
                .then()
                .statusCode(200)
                .body("data.id", equalTo(PICHINCHA_ID));

        given()
                .when().get(basePath + "/" + QUITO_ID)
                .then()
                .statusCode(200)
                .body("data.parentDivisionId", equalTo(PICHINCHA_ID));

        given()
                .when().get(basePath + "/" + PICHINCHA_ID + "/identifiers")
                .then()
                .statusCode(200)
                .body("data", hasSize(2))
                .body("data.schemeCode", contains("EC_INEC_DPA", "ISO_3166_2"));

        given()
                .when().get(basePath + "/" + PICHINCHA_ID + "/names")
                .then()
                .statusCode(200)
                .body("data", hasSize(2))
                .body("data.name", contains("Pichincha Province", "Provincia de Pichincha"));
    }

    @Test
    void shouldRejectUnsupportedAdministrativeDivisionFilters() {
        final String basePath = "/api/v1/countries/" + ECUADOR_ID + "/administrative-divisions";

        assertError(basePath + "?root=false", 400, "invalid-filter-combination");
        assertError(basePath + "?status=ACTIVE", 400, "invalid-filter-combination");
        assertError(basePath + "?typeId=" + PROVINCE_TYPE_ID, 400, "invalid-filter-combination");
        assertError(
                basePath + "?parentId=" + PICHINCHA_ID + "&root=true",
                400,
                "invalid-filter-combination"
        );
        assertError(basePath + "?parentId=bad-id", 400, "invalid-uuid");
        assertError(basePath + "?typeId=bad-id&status=ACTIVE", 400, "invalid-uuid");
        assertError(
                basePath + "?typeId=" + PROVINCE_TYPE_ID + "&status=unknown",
                400,
                "invalid-status"
        );
    }

    @Test
    void shouldValidateAdministrativeDivisionContextAndAbsence() {
        final String ecuadorPath = "/api/v1/countries/" + ECUADOR_ID + "/administrative-divisions";
        final String colombiaPath = "/api/v1/countries/" + COLOMBIA_ID + "/administrative-divisions";

        assertError(
                "/api/v1/countries/" + UNKNOWN_ID + "/administrative-divisions",
                404,
                "country-not-found"
        );
        assertError(
                "/api/v1/countries/" + UNKNOWN_ID + "/administrative-divisions/" + PICHINCHA_ID,
                404,
                "country-not-found"
        );
        assertError(
                ecuadorPath + "/by-canonical-code/unknown",
                404,
                "administrative-division-not-found"
        );
        assertError(ecuadorPath + "/" + UNKNOWN_ID, 404, "administrative-division-not-found");
        assertError(colombiaPath + "/" + PICHINCHA_ID, 404, "administrative-division-not-found");
        assertError(
                colombiaPath + "/" + PICHINCHA_ID + "/identifiers",
                404,
                "administrative-division-not-found"
        );
        assertError(
                colombiaPath + "/" + PICHINCHA_ID + "/names",
                404,
                "administrative-division-not-found"
        );
    }

    @Test
    void shouldValidateAndPropagateRequestLoggingHeaders() {
        given()
                .header(PROCESS_ID_HEADER, "")
                .when().get("/api/v1/countries")
                .then()
                .statusCode(200)
                .header(
                        PROCESS_ID_HEADER,
                        matchesPattern("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"));

        given()
                .header(PROCESS_ID_HEADER, "not-a-uuid")
                .when().get("/api/v1/countries")
                .then()
                .statusCode(400)
                .body("code", equalTo("bad-request"));

        given()
                .header(USER_ID_HEADER, " ")
                .when().get("/api/v1/countries")
                .then()
                .statusCode(400)
                .header(PROCESS_ID_HEADER, equalTo(PROCESS_ID))
                .body("code", equalTo("bad-request"));

        given()
                .header(USER_ID_HEADER, "x".repeat(129))
                .when().get("/api/v1/countries")
                .then()
                .statusCode(400)
                .body("code", equalTo("bad-request"));

        given()
                .header(COMPANY_ID_HEADER, "not-a-uuid")
                .when().get("/api/v1/countries")
                .then()
                .statusCode(400)
                .body("code", equalTo("bad-request"));
    }

    @Test
    void shouldExposeOpenApiAndSwaggerUiInTestMode() {
        given()
                .accept("application/json")
                .when().get("/q/openapi")
                .then()
                .statusCode(200)
                .body("info.title", equalTo("Geographic Reference API"))
                .body("info.version", equalTo("1.0.0"))
                .body(
                        "paths.'/api/v1/countries'.get.parameters.find { it.name == 'process-id' }.schema.format",
                        equalTo("uuid"))
                .body(
                        "paths.'/api/v1/countries'.get.parameters.find { it.name == 'user-id' }.required",
                        equalTo(true))
                .body(
                        "paths.'/api/v1/countries'.get.parameters.find { it.name == 'user-id' }.schema.maxLength",
                        equalTo(128))
                .body(
                        "paths.'/api/v1/countries'.get.parameters.find { it.name == 'company-id' }.schema.format",
                        equalTo("uuid"))
                .body(
                        "paths.'/api/v1/countries'.get.responses.'200'.headers.'process-id'.schema.format",
                        equalTo("uuid"))
                .body(
                        "paths.'/api/v1/countries'.get.responses.'200'.content.'application/json'.schema.'$ref'",
                        equalTo("#/components/schemas/CountryListResponse"))
                .body("components.schemas.CountryListResponse.properties.data.type", equalTo("array"))
                .body(
                        "components.schemas.CountryListResponse.properties.data.items.'$ref'",
                        equalTo("#/components/schemas/CountryApiResponse"));

        given()
                .redirects().follow(false)
                .when().get("/q/swagger-ui")
                .then()
                .statusCode(302);
    }

    @Test
    void shouldWrapMethodNotAllowedResponses() {
        given()
                .when().post("/api/v1/countries")
                .then()
                .statusCode(405)
                .body("status", equalTo(405))
                .body("code", equalTo("method-not-allowed"))
                .body("$", not(hasKey("data")));

        assertError("/api/v1/does-not-exist", 404, "not-found");
    }

    private void assertError(final String path, final int status, final String code) {
        given()
                .when().get(path)
                .then()
                .statusCode(status)
                .body("status", equalTo(status))
                .body("code", equalTo(code))
                .body("$", not(hasKey("data")));
    }
}

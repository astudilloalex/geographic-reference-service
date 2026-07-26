package com.alexastudillo.geographicreference.infrastructure.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;

@QuarkusTest
class GeographicReferenceApiTest {

    private static final String ECUADOR_ID = "00000000-0000-7000-8000-000000000218";
    private static final String COLOMBIA_ID = "00000000-0000-7000-8000-000000000170";
    private static final String PROVINCE_TYPE_ID = "20000000-0000-7000-8000-000000000001";
    private static final String COLOMBIA_TYPE_ID = "20000000-0000-7000-8000-000000000003";
    private static final String PICHINCHA_ID = "30000000-0000-7000-8000-000000000002";
    private static final String QUITO_ID = "30000000-0000-7000-8000-000000000003";
    private static final String UNKNOWN_ID = "90000000-0000-7000-8000-000000000099";

    @Test
    void shouldListAndFilterCountriesWithStandardEnvelope() {
        given()
                .when().get("/api/v1/countries")
                .then()
                .statusCode(200)
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
    void shouldRejectInvalidCountryParametersAndReportMissingCountries() {
        assertError("/api/v1/countries/not-a-uuid", 400, "invalid-uuid");
        assertError("/api/v1/countries/by-alpha2/E", 400, "bad-request");
        assertError("/api/v1/countries/by-alpha3/E1U", 400, "bad-request");
        assertError("/api/v1/countries/by-numeric/2A8", 400, "bad-request");
        assertError("/api/v1/countries?status=unknown", 400, "invalid-status");
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
    void shouldExposeOpenApiAndSwaggerUiInTestMode() {
        given()
                .accept("application/json")
                .when().get("/q/openapi")
                .then()
                .statusCode(200)
                .body("info.title", equalTo("Geographic Reference API"))
                .body("info.version", equalTo("1.0.0"));

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

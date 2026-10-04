package com.alexastudillo.geographicreference.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItems;

@QuarkusTest
class HealthApiTest {

    @Test
    void shouldExposeLivenessWithoutRequestHeaders() {
        given()
                .when().get("/q/health/live")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    void shouldValidateJdbcAndReactiveDatasourcesBeforeReportingReady() {
        given()
                .when().get("/q/health/ready")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("checks.name", hasItems(
                        "Database connections health check",
                        "Reactive PostgreSQL connections health check"))
                .body("checks.status", everyItem(equalTo("UP")))
                .body("checks.data.'<default>'", everyItem(equalTo("UP")));
    }
}

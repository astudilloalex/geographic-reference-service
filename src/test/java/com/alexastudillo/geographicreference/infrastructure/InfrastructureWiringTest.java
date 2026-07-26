package com.alexastudillo.geographicreference.infrastructure;

import com.alexastudillo.geographicreference.application.port.input.GetAdministrativeDivisionQueryPort;
import com.alexastudillo.geographicreference.application.port.input.GetAdministrativeDivisionTypeQueryPort;
import com.alexastudillo.geographicreference.application.port.input.GetCountryQueryPort;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.mutiny.sqlclient.Pool;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class InfrastructureWiringTest {

    @Inject
    GetCountryQueryPort countryQueryPort;

    @Inject
    GetAdministrativeDivisionTypeQueryPort divisionTypeQueryPort;

    @Inject
    GetAdministrativeDivisionQueryPort divisionQueryPort;

    @Inject
    Pool pool;

    @Test
    void shouldComposeFrameworkIndependentApplicationServices() {
        assertThat(countryQueryPort.findByAlpha2Code("ec").await().indefinitely().alpha2Code()).isEqualTo("EC");
        assertThat(divisionTypeQueryPort
                .listByCountryId(java.util.UUID.fromString("00000000-0000-7000-8000-000000000218"))
                .await().indefinitely()).hasSize(2);
        assertThat(divisionQueryPort
                .findByCanonicalCode(
                        java.util.UUID.fromString("00000000-0000-7000-8000-000000000218"),
                        "ec-p"
                )
                .await().indefinitely().canonicalCode()).isEqualTo("EC-P");
    }

    @Test
    void shouldApplyProductionAndTestFlywayMigrations() {
        final List<String> versions = pool.query("""
                        SELECT version
                          FROM flyway_schema_history
                         WHERE success
                         ORDER BY installed_rank
                        """)
                .execute()
                .onItem().transform(rows -> {
                    final java.util.ArrayList<String> result = new java.util.ArrayList<>();
                    rows.forEach(row -> result.add(row.getString("version")));
                    return List.copyOf(result);
                })
                .await().indefinitely();

        assertThat(versions).containsExactly("1.0.0", "1.0.1");
    }
}

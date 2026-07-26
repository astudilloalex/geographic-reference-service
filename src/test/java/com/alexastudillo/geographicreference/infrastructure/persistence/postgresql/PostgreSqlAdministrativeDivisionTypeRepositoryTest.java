package com.alexastudillo.geographicreference.infrastructure.persistence.postgresql;

import com.alexastudillo.geographicreference.application.port.output.AdministrativeDivisionTypeRepository;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivisionType;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionTypeId;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.mutiny.sqlclient.Pool;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class PostgreSqlAdministrativeDivisionTypeRepositoryTest {

    private static final CountryId ECUADOR_ID =
            CountryId.of(UUID.fromString("00000000-0000-7000-8000-000000000218"));
    private static final DivisionTypeId PROVINCE_ID =
            DivisionTypeId.of(UUID.fromString("20000000-0000-7000-8000-000000000001"));

    @Inject
    Pool pool;

    AdministrativeDivisionTypeRepository repository;

    @BeforeEach
    void setUp() {
        repository = new PostgreSqlAdministrativeDivisionTypeRepository(
                pool,
                new AdministrativeDivisionTypeRowMapper(),
                new ReactiveRowSetMapper()
        );
    }

    @Test
    void shouldFindDivisionTypeByIdAndCodeAndMapAllFields() {
        final AdministrativeDivisionType type = repository.findById(PROVINCE_ID).await().indefinitely();

        assertThat(type).isNotNull();
        assertThat(type.id()).isEqualTo(PROVINCE_ID);
        assertThat(type.countryId()).isEqualTo(ECUADOR_ID);
        assertThat(type.code()).isEqualTo("PROVINCE");
        assertThat(type.name()).isEqualTo("Province");
        assertThat(type.hierarchyLevel()).isEqualTo((short) 1);
        assertThat(type.status()).isEqualTo(GeographicRecordStatus.ACTIVE);
        assertThat(type.auditInfo().version()).isEqualTo(3);
        assertThat(repository.findByCountryIdAndCode(ECUADOR_ID, "PROVINCE").await().indefinitely())
                .isEqualTo(type);
        assertThat(repository.findById(DivisionTypeId.of(UUID.randomUUID())).await().indefinitely()).isNull();
    }

    @Test
    void shouldReturnFiniteOrderedAndFilteredTypeLists() {
        final List<AdministrativeDivisionType> types = repository.findByCountryId(ECUADOR_ID)
                .await().indefinitely();

        assertThat(types).extracting(AdministrativeDivisionType::code).containsExactly("PROVINCE", "CANTON");
        assertThat(repository.findByCountryIdAndStatus(ECUADOR_ID, GeographicRecordStatus.ACTIVE)
                .await().indefinitely()).hasSize(2);
        assertThat(repository.findByCountryIdAndStatus(ECUADOR_ID, GeographicRecordStatus.DRAFT)
                .await().indefinitely()).isEmpty();
    }
}

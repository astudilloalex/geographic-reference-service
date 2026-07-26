package com.alexastudillo.geographicreference.infrastructure.persistence.postgresql;

import com.alexastudillo.geographicreference.application.port.output.AdministrativeDivisionRepository;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivision;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivisionIdentifier;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivisionName;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicIdentifierStatus;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicNameType;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionId;
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
class PostgreSqlAdministrativeDivisionRepositoryTest {

    private static final CountryId ECUADOR_ID =
            CountryId.of(UUID.fromString("00000000-0000-7000-8000-000000000218"));
    private static final DivisionTypeId PROVINCE_TYPE_ID =
            DivisionTypeId.of(UUID.fromString("20000000-0000-7000-8000-000000000001"));
    private static final DivisionTypeId CANTON_TYPE_ID =
            DivisionTypeId.of(UUID.fromString("20000000-0000-7000-8000-000000000002"));
    private static final DivisionId GUAYAS_ID =
            DivisionId.of(UUID.fromString("30000000-0000-7000-8000-000000000001"));
    private static final DivisionId PICHINCHA_ID =
            DivisionId.of(UUID.fromString("30000000-0000-7000-8000-000000000002"));
    private static final DivisionId QUITO_ID =
            DivisionId.of(UUID.fromString("30000000-0000-7000-8000-000000000003"));

    @Inject
    Pool pool;

    AdministrativeDivisionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new PostgreSqlAdministrativeDivisionRepository(
                pool,
                new AdministrativeDivisionRowMapper(),
                new ReactiveRowSetMapper()
        );
    }

    @Test
    void shouldFindDivisionByIdAndCanonicalCodeAndMapAllFields() {
        final AdministrativeDivision division = repository.findById(PICHINCHA_ID).await().indefinitely();

        assertThat(division).isNotNull();
        assertThat(division.id()).isEqualTo(PICHINCHA_ID);
        assertThat(division.countryId()).isEqualTo(ECUADOR_ID);
        assertThat(division.divisionTypeId()).isEqualTo(PROVINCE_TYPE_ID);
        assertThat(division.parentDivisionId()).isNull();
        assertThat(division.isRoot()).isTrue();
        assertThat(division.canonicalCode()).isEqualTo("EC-P");
        assertThat(division.defaultName()).isEqualTo("Pichincha");
        assertThat(division.officialName()).isEqualTo("Provincia de Pichincha");
        assertThat(division.status()).isEqualTo(GeographicRecordStatus.ACTIVE);
        assertThat(division.sourceProvenance().reference()).isEqualTo("ISO 3166-2:EC");
        assertThat(division.sourceProvenance().revision()).isEqualTo("2025");
        assertThat(division.auditInfo().version()).isEqualTo(4);
        assertThat(repository.findByCanonicalCode(ECUADOR_ID, "EC-P").await().indefinitely())
                .isEqualTo(division);
        assertThat(repository.findById(DivisionId.of(UUID.randomUUID())).await().indefinitely()).isNull();

        final AdministrativeDivision child = repository.findById(QUITO_ID).await().indefinitely();
        assertThat(child.parentDivisionId()).isEqualTo(PICHINCHA_ID);
        assertThat(child.isRoot()).isFalse();
    }

    @Test
    void shouldReturnOrderedDivisionListsForCountryParentTypeAndStatus() {
        final List<AdministrativeDivision> divisions = repository.findByCountryId(ECUADOR_ID)
                .await().indefinitely();

        assertThat(divisions)
                .extracting(AdministrativeDivision::canonicalCode)
                .containsExactly("EC-G", "EC-P", "EC-P-Q");
        assertThat(repository.findByParentDivisionId(ECUADOR_ID, null).await().indefinitely())
                .extracting(AdministrativeDivision::canonicalCode)
                .containsExactly("EC-G", "EC-P");
        assertThat(repository.findByParentDivisionId(ECUADOR_ID, PICHINCHA_ID).await().indefinitely())
                .extracting(AdministrativeDivision::canonicalCode)
                .containsExactly("EC-P-Q");
        assertThat(repository.findByParentDivisionId(ECUADOR_ID, GUAYAS_ID).await().indefinitely()).isEmpty();
        assertThat(repository.findByTypeAndStatus(
                ECUADOR_ID,
                PROVINCE_TYPE_ID,
                GeographicRecordStatus.ACTIVE
        ).await().indefinitely())
                .extracting(AdministrativeDivision::canonicalCode)
                .containsExactly("EC-G", "EC-P");
        assertThat(repository.findByTypeAndStatus(
                ECUADOR_ID,
                CANTON_TYPE_ID,
                GeographicRecordStatus.DRAFT
        ).await().indefinitely()).isEmpty();
    }

    @Test
    void shouldFindOrderedIdentifiersAndMapAllFields() {
        final List<AdministrativeDivisionIdentifier> identifiers =
                repository.findIdentifiersByDivisionId(ECUADOR_ID, PICHINCHA_ID).await().indefinitely();

        assertThat(identifiers).hasSize(2);
        assertThat(identifiers).extracting(AdministrativeDivisionIdentifier::schemeCode)
                .containsExactly("EC_INEC_DPA", "ISO_3166_2");
        assertThat(identifiers.getFirst().countryId()).isEqualTo(ECUADOR_ID);
        assertThat(identifiers.getFirst().divisionId()).isEqualTo(PICHINCHA_ID);
        assertThat(identifiers.getFirst().identifierValue()).isEqualTo("17");
        assertThat(identifiers.getFirst().primary()).isTrue();
        assertThat(identifiers.getFirst().status()).isEqualTo(GeographicIdentifierStatus.ACTIVE);
        assertThat(identifiers.getFirst().sourceProvenance().authority()).isEqualTo("INEC");
        assertThat(identifiers.getFirst().sourceProvenance().revision()).isNull();
        assertThat(repository.findIdentifiersByDivisionId(ECUADOR_ID, GUAYAS_ID).await().indefinitely()).isEmpty();
    }

    @Test
    void shouldFindOrderedNamesAndMapAllFields() {
        final List<AdministrativeDivisionName> names =
                repository.findNamesByDivisionId(ECUADOR_ID, PICHINCHA_ID).await().indefinitely();

        assertThat(names).hasSize(2);
        assertThat(names).extracting(name -> name.languageTag().value()).containsExactly("en", "es");
        assertThat(names.getFirst().nameType()).isEqualTo(GeographicNameType.COMMON);
        assertThat(names.getFirst().name()).isEqualTo("Pichincha Province");
        assertThat(names.getFirst().preferred()).isTrue();
        assertThat(names.getLast().auditInfo().version()).isEqualTo(2);
        assertThat(repository.findNamesByDivisionId(ECUADOR_ID, GUAYAS_ID).await().indefinitely()).isEmpty();
    }
}

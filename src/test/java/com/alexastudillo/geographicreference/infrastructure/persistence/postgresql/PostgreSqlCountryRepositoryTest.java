package com.alexastudillo.geographicreference.infrastructure.persistence.postgresql;

import com.alexastudillo.geographicreference.application.port.output.CountryRepository;
import com.alexastudillo.geographicreference.domain.model.entity.Country;
import com.alexastudillo.geographicreference.domain.model.entity.CountryName;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicNameType;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.Alpha2Code;
import com.alexastudillo.geographicreference.domain.model.valobj.Alpha3Code;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.NumericCode;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.mutiny.sqlclient.Pool;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class PostgreSqlCountryRepositoryTest {

    private static final CountryId ECUADOR_ID =
            CountryId.of(UUID.fromString("00000000-0000-7000-8000-000000000218"));
    private static final CountryId COLOMBIA_ID =
            CountryId.of(UUID.fromString("00000000-0000-7000-8000-000000000170"));

    @Inject
    Pool pool;

    CountryRepository repository;

    @BeforeEach
    void setUp() {
        repository = new PostgreSqlCountryRepository(
                pool,
                new CountryRowMapper(),
                new ReactiveRowSetMapper()
        );
    }

    @Test
    void shouldFindCountryByEverySupportedKeyAndMapAllFields() {
        final Country country = repository.findById(ECUADOR_ID).await().indefinitely();

        assertThat(country).isNotNull();
        assertThat(country.id()).isEqualTo(ECUADOR_ID);
        assertThat(country.alpha2Code().value()).isEqualTo("EC");
        assertThat(country.alpha3Code().value()).isEqualTo("ECU");
        assertThat(country.numericCode().value()).isEqualTo("218");
        assertThat(country.defaultName()).isEqualTo("Ecuador");
        assertThat(country.officialName()).isEqualTo("Republic of Ecuador");
        assertThat(country.independent()).isTrue();
        assertThat(country.status()).isEqualTo(GeographicRecordStatus.ACTIVE);
        assertThat(country.validityPeriod().validFrom()).isEqualTo(LocalDate.parse("1830-05-13"));
        assertThat(country.validityPeriod().validUntil()).isNull();
        assertThat(country.sourceProvenance().authority()).isEqualTo("ISO");
        assertThat(country.sourceProvenance().reference()).isEqualTo("ISO 3166-1");
        assertThat(country.sourceProvenance().revision()).isEqualTo("2025");
        assertThat(country.auditInfo().createdAt()).isEqualTo(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        assertThat(country.auditInfo().createdBy()).isEqualTo("test-fixture");
        assertThat(country.auditInfo().updatedAt()).isEqualTo(OffsetDateTime.parse("2026-01-02T00:00:00Z"));
        assertThat(country.auditInfo().updatedBy()).isEqualTo("test-fixture");
        assertThat(country.auditInfo().version()).isEqualTo(2);

        assertThat(repository.findByAlpha2Code(Alpha2Code.of("EC")).await().indefinitely()).isEqualTo(country);
        assertThat(repository.findByAlpha3Code(Alpha3Code.of("ECU")).await().indefinitely()).isEqualTo(country);
        assertThat(repository.findByNumericCode(NumericCode.of("218")).await().indefinitely()).isEqualTo(country);
        assertThat(repository.findById(CountryId.of(UUID.randomUUID())).await().indefinitely()).isNull();
    }

    @Test
    void shouldReturnFiniteOrderedCountryLists() {
        final List<Country> countries = repository.findAll().await().indefinitely();
        final List<Country> activeCountries = repository.findByStatus(GeographicRecordStatus.ACTIVE)
                .await().indefinitely();

        assertThat(countries)
                .extracting(country -> country.alpha2Code().value())
                .containsExactly("CO", "EC");
        assertThat(activeCountries)
                .extracting(country -> country.alpha2Code().value())
                .containsExactly("EC");
        assertThat(repository.findByStatus(GeographicRecordStatus.DRAFT).await().indefinitely()).isEmpty();
    }

    @Test
    void shouldFindOrderedCountryNamesAndMapAllFields() {
        final List<CountryName> names = repository.findNamesByCountryId(ECUADOR_ID).await().indefinitely();

        assertThat(names).hasSize(2);
        assertThat(names).extracting(name -> name.languageTag().value()).containsExactly("en", "es");
        assertThat(names.getFirst().countryId()).isEqualTo(ECUADOR_ID);
        assertThat(names.getFirst().nameType()).isEqualTo(GeographicNameType.OFFICIAL);
        assertThat(names.getFirst().name()).isEqualTo("Republic of Ecuador");
        assertThat(names.getFirst().preferred()).isTrue();
        assertThat(names.getFirst().validityPeriod().validFrom()).isEqualTo(LocalDate.parse("1830-05-13"));
        assertThat(names.getFirst().auditInfo().version()).isZero();
        assertThat(repository.findNamesByCountryId(COLOMBIA_ID).await().indefinitely()).isEmpty();
    }
}

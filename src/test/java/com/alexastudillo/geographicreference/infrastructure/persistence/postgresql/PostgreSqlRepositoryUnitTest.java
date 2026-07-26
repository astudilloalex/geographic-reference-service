package com.alexastudillo.geographicreference.infrastructure.persistence.postgresql;

import com.alexastudillo.geographicreference.domain.model.enums.CountryCodeType;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicNameType;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.Alpha2Code;
import com.alexastudillo.geographicreference.domain.model.valobj.Alpha3Code;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionTypeId;
import com.alexastudillo.geographicreference.domain.model.valobj.NumericCode;
import com.alexastudillo.geographicreference.domain.model.valobj.LanguageTag;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.PreparedQuery;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostgreSqlRepositoryUnitTest {

    private static final CountryId COUNTRY_ID = CountryId.of(UUID.randomUUID());
    private static final DivisionTypeId TYPE_ID = DivisionTypeId.of(UUID.randomUUID());
    private static final DivisionId DIVISION_ID = DivisionId.of(UUID.randomUUID());

    @Mock
    Pool pool;

    @Mock
    PreparedQuery<RowSet<Row>> query;

    @Mock
    RowSet<Row> rows;

    @Mock
    ReactiveRowSetMapper rowSetMapper;

    @Mock
    CountryRowMapper countryRowMapper;

    @Mock
    AdministrativeDivisionTypeRowMapper divisionTypeRowMapper;

    @Mock
    AdministrativeDivisionRowMapper divisionRowMapper;

    @Test
    void shouldExecuteEveryCountryQueryThroughTheReactiveClient() {
        when(pool.preparedQuery(any(String.class))).thenReturn(query);
        when(query.execute(any(Tuple.class))).thenReturn(Uni.createFrom().item(rows));
        when(query.execute()).thenReturn(Uni.createFrom().item(rows));
        final PostgreSqlCountryRepository repository =
                new PostgreSqlCountryRepository(pool, countryRowMapper, rowSetMapper);

        assertThat(repository.findById(COUNTRY_ID).await().indefinitely()).isNull();
        assertThat(repository.findByAlpha2Code(Alpha2Code.of("EC")).await().indefinitely()).isNull();
        assertThat(repository.findByAlpha3Code(Alpha3Code.of("ECU")).await().indefinitely()).isNull();
        assertThat(repository.findByNumericCode(NumericCode.of("218")).await().indefinitely()).isNull();
        assertThat(repository.findByStatus(GeographicRecordStatus.ACTIVE).await().indefinitely()).isEmpty();
        assertThat(repository.findAll().await().indefinitely()).isEmpty();
        assertThat(repository.findNamesByCountryId(COUNTRY_ID).await().indefinitely()).isEmpty();
        assertThat(repository.findNames(
                CountryCodeType.ALPHA2,
                GeographicNameType.COMMON,
                LanguageTag.of("es"))
                .await().indefinitely()).isEmpty();
    }

    @Test
    void shouldExecuteEveryDivisionTypeQueryThroughTheReactiveClient() {
        when(pool.preparedQuery(any(String.class))).thenReturn(query);
        when(query.execute(any(Tuple.class))).thenReturn(Uni.createFrom().item(rows));
        final PostgreSqlAdministrativeDivisionTypeRepository repository =
                new PostgreSqlAdministrativeDivisionTypeRepository(pool, divisionTypeRowMapper, rowSetMapper);

        assertThat(repository.findById(TYPE_ID).await().indefinitely()).isNull();
        assertThat(repository.findByCountryIdAndCode(COUNTRY_ID, "PROVINCE").await().indefinitely()).isNull();
        assertThat(repository.findByCountryId(COUNTRY_ID).await().indefinitely()).isEmpty();
        assertThat(repository.findByCountryIdAndStatus(COUNTRY_ID, GeographicRecordStatus.ACTIVE)
                .await().indefinitely()).isEmpty();
    }

    @Test
    void shouldExecuteEveryDivisionQueryThroughTheReactiveClient() {
        when(pool.preparedQuery(any(String.class))).thenReturn(query);
        when(query.execute(any(Tuple.class))).thenReturn(Uni.createFrom().item(rows));
        final PostgreSqlAdministrativeDivisionRepository repository =
                new PostgreSqlAdministrativeDivisionRepository(pool, divisionRowMapper, rowSetMapper);

        assertThat(repository.findById(DIVISION_ID).await().indefinitely()).isNull();
        assertThat(repository.findByCanonicalCode(COUNTRY_ID, "EC-P").await().indefinitely()).isNull();
        assertThat(repository.findByCountryId(COUNTRY_ID).await().indefinitely()).isEmpty();
        assertThat(repository.findByParentDivisionId(COUNTRY_ID, null).await().indefinitely()).isEmpty();
        assertThat(repository.findByParentDivisionId(COUNTRY_ID, DIVISION_ID).await().indefinitely()).isEmpty();
        assertThat(repository.findByTypeAndStatus(COUNTRY_ID, TYPE_ID, GeographicRecordStatus.ACTIVE)
                .await().indefinitely()).isEmpty();
        assertThat(repository.findIdentifiersByDivisionId(COUNTRY_ID, DIVISION_ID)
                .await().indefinitely()).isEmpty();
        assertThat(repository.findNamesByDivisionId(COUNTRY_ID, DIVISION_ID).await().indefinitely()).isEmpty();
    }

    @Test
    void shouldLogAndPropagateReactiveRepositoryFailures() {
        final IllegalStateException failure = new IllegalStateException("database unavailable");
        when(pool.preparedQuery(any(String.class))).thenReturn(query);
        when(query.execute(any(Tuple.class))).thenReturn(Uni.createFrom().failure(failure));

        final PostgreSqlCountryRepository countryRepository =
                new PostgreSqlCountryRepository(pool, countryRowMapper, rowSetMapper);
        final PostgreSqlAdministrativeDivisionTypeRepository divisionTypeRepository =
                new PostgreSqlAdministrativeDivisionTypeRepository(pool, divisionTypeRowMapper, rowSetMapper);
        final PostgreSqlAdministrativeDivisionRepository divisionRepository =
                new PostgreSqlAdministrativeDivisionRepository(pool, divisionRowMapper, rowSetMapper);

        assertThatThrownBy(() -> countryRepository.findById(COUNTRY_ID).await().indefinitely())
                .isSameAs(failure);
        assertThatThrownBy(() -> divisionTypeRepository.findById(TYPE_ID).await().indefinitely())
                .isSameAs(failure);
        assertThatThrownBy(() -> divisionRepository.findById(DIVISION_ID).await().indefinitely())
                .isSameAs(failure);
    }
}

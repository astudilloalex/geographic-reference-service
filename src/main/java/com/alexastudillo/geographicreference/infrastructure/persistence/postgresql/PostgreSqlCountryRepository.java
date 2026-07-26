package com.alexastudillo.geographicreference.infrastructure.persistence.postgresql;

import com.alexastudillo.geographicreference.application.port.output.CountryRepository;
import com.alexastudillo.geographicreference.domain.model.entity.Country;
import com.alexastudillo.geographicreference.domain.model.entity.CountryName;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.Alpha2Code;
import com.alexastudillo.geographicreference.domain.model.valobj.Alpha3Code;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.NumericCode;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Objects;

/**
 * Reactive PostgreSQL adapter for country read operations.
 */
@ApplicationScoped
public class PostgreSqlCountryRepository implements CountryRepository {

    private static final String SELECT_COUNTRY = """
            SELECT id,
                   alpha2_code,
                   alpha3_code,
                   numeric_code,
                   default_name,
                   official_name,
                   is_independent,
                   status::text AS status,
                   valid_from,
                   valid_until,
                   source_authority,
                   source_reference,
                   source_revision,
                   created_at,
                   created_by,
                   updated_at,
                   updated_by,
                   version
              FROM countries
            """;

    private static final String FIND_BY_ID = SELECT_COUNTRY + " WHERE id = $1";
    private static final String FIND_BY_ALPHA2 = SELECT_COUNTRY + " WHERE alpha2_code = $1";
    private static final String FIND_BY_ALPHA3 = SELECT_COUNTRY + " WHERE alpha3_code = $1";
    private static final String FIND_BY_NUMERIC = SELECT_COUNTRY + " WHERE numeric_code = $1";
    private static final String FIND_BY_STATUS = SELECT_COUNTRY
            + " WHERE status = $1::geographic_record_status ORDER BY alpha2_code";
    private static final String FIND_ALL = SELECT_COUNTRY + " ORDER BY alpha2_code";

    private static final String FIND_NAMES_BY_COUNTRY_ID = """
            SELECT id,
                   country_id,
                   language_tag,
                   name_type::text AS name_type,
                   name,
                   is_preferred,
                   valid_from,
                   valid_until,
                   created_at,
                   created_by,
                   updated_at,
                   updated_by,
                   version
              FROM country_names
             WHERE country_id = $1
             ORDER BY language_tag, name_type, name, id
            """;

    private final Pool pool;
    private final CountryRowMapper rowMapper;
    private final ReactiveRowSetMapper rowSetMapper;

    public PostgreSqlCountryRepository(
            final Pool pool,
            final CountryRowMapper rowMapper,
            final ReactiveRowSetMapper rowSetMapper
    ) {
        this.pool = Objects.requireNonNull(pool, "Pool must not be null");
        this.rowMapper = Objects.requireNonNull(rowMapper, "CountryRowMapper must not be null");
        this.rowSetMapper = Objects.requireNonNull(rowSetMapper, "ReactiveRowSetMapper must not be null");
    }

    @Override
    public Uni<Country> findById(final CountryId id) {
        return pool.preparedQuery(FIND_BY_ID)
                .execute(Tuple.of(id.value()))
                .onItem().transform(rows -> rowSetMapper.firstOrNull(rows, rowMapper::toCountry));
    }

    @Override
    public Uni<Country> findByAlpha2Code(final Alpha2Code code) {
        return pool.preparedQuery(FIND_BY_ALPHA2)
                .execute(Tuple.of(code.value()))
                .onItem().transform(rows -> rowSetMapper.firstOrNull(rows, rowMapper::toCountry));
    }

    @Override
    public Uni<Country> findByAlpha3Code(final Alpha3Code code) {
        return pool.preparedQuery(FIND_BY_ALPHA3)
                .execute(Tuple.of(code.value()))
                .onItem().transform(rows -> rowSetMapper.firstOrNull(rows, rowMapper::toCountry));
    }

    @Override
    public Uni<Country> findByNumericCode(final NumericCode code) {
        return pool.preparedQuery(FIND_BY_NUMERIC)
                .execute(Tuple.of(code.value()))
                .onItem().transform(rows -> rowSetMapper.firstOrNull(rows, rowMapper::toCountry));
    }

    @Override
    public Uni<List<Country>> findByStatus(final GeographicRecordStatus status) {
        return pool.preparedQuery(FIND_BY_STATUS)
                .execute(Tuple.of(status.name()))
                .onItem().transform(rows -> rowSetMapper.toList(rows, rowMapper::toCountry));
    }

    @Override
    public Uni<List<Country>> findAll() {
        return pool.preparedQuery(FIND_ALL)
                .execute()
                .onItem().transform(rows -> rowSetMapper.toList(rows, rowMapper::toCountry));
    }

    @Override
    public Uni<List<CountryName>> findNamesByCountryId(final CountryId countryId) {
        return pool.preparedQuery(FIND_NAMES_BY_COUNTRY_ID)
                .execute(Tuple.of(countryId.value()))
                .onItem().transform(rows -> rowSetMapper.toList(rows, rowMapper::toCountryName));
    }
}

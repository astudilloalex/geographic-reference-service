package com.alexastudillo.geographicreference.infrastructure.persistence.postgresql;

import com.alexastudillo.geographicreference.application.port.output.AdministrativeDivisionTypeRepository;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivisionType;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionTypeId;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Objects;

/**
 * Reactive PostgreSQL adapter for administrative division type reads.
 */
@ApplicationScoped
public class PostgreSqlAdministrativeDivisionTypeRepository implements AdministrativeDivisionTypeRepository {

    private static final String SELECT_TYPE = """
            SELECT id,
                   country_id,
                   code,
                   name,
                   hierarchy_level,
                   status::text AS status,
                   created_at,
                   created_by,
                   updated_at,
                   updated_by,
                   version
              FROM administrative_division_types
            """;

    private static final String FIND_BY_ID = SELECT_TYPE + " WHERE id = $1";
    private static final String FIND_BY_COUNTRY_AND_CODE = SELECT_TYPE
            + " WHERE country_id = $1 AND code = $2";
    private static final String FIND_BY_COUNTRY = SELECT_TYPE
            + " WHERE country_id = $1 ORDER BY hierarchy_level, code";
    private static final String FIND_BY_COUNTRY_AND_STATUS = SELECT_TYPE
            + """
               WHERE country_id = $1
                 AND status = $2::geographic_record_status
               ORDER BY hierarchy_level, code
              """;

    private final Pool pool;
    private final AdministrativeDivisionTypeRowMapper rowMapper;
    private final ReactiveRowSetMapper rowSetMapper;

    public PostgreSqlAdministrativeDivisionTypeRepository(
            final Pool pool,
            final AdministrativeDivisionTypeRowMapper rowMapper,
            final ReactiveRowSetMapper rowSetMapper
    ) {
        this.pool = Objects.requireNonNull(pool, "Pool must not be null");
        this.rowMapper = Objects.requireNonNull(rowMapper, "AdministrativeDivisionTypeRowMapper must not be null");
        this.rowSetMapper = Objects.requireNonNull(rowSetMapper, "ReactiveRowSetMapper must not be null");
    }

    @Override
    public Uni<AdministrativeDivisionType> findById(final DivisionTypeId id) {
        return pool.preparedQuery(FIND_BY_ID)
                .execute(Tuple.of(id.value()))
                .onItem().transform(rows -> rowSetMapper.firstOrNull(
                        rows,
                        rowMapper::toAdministrativeDivisionType
                ));
    }

    @Override
    public Uni<AdministrativeDivisionType> findByCountryIdAndCode(final CountryId countryId, final String code) {
        return pool.preparedQuery(FIND_BY_COUNTRY_AND_CODE)
                .execute(Tuple.of(countryId.value(), code))
                .onItem().transform(rows -> rowSetMapper.firstOrNull(
                        rows,
                        rowMapper::toAdministrativeDivisionType
                ));
    }

    @Override
    public Uni<List<AdministrativeDivisionType>> findByCountryId(final CountryId countryId) {
        return pool.preparedQuery(FIND_BY_COUNTRY)
                .execute(Tuple.of(countryId.value()))
                .onItem().transform(rows -> rowSetMapper.toList(
                        rows,
                        rowMapper::toAdministrativeDivisionType
                ));
    }

    @Override
    public Uni<List<AdministrativeDivisionType>> findByCountryIdAndStatus(
            final CountryId countryId,
            final GeographicRecordStatus status
    ) {
        return pool.preparedQuery(FIND_BY_COUNTRY_AND_STATUS)
                .execute(Tuple.of(countryId.value(), status.name()))
                .onItem().transform(rows -> rowSetMapper.toList(
                        rows,
                        rowMapper::toAdministrativeDivisionType
                ));
    }
}

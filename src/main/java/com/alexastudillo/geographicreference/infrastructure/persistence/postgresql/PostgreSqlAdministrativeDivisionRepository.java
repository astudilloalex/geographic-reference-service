package com.alexastudillo.geographicreference.infrastructure.persistence.postgresql;

import com.alexastudillo.geographicreference.application.port.output.AdministrativeDivisionRepository;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivision;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivisionIdentifier;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivisionName;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionTypeId;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Objects;

/**
 * Reactive PostgreSQL adapter for administrative division reads.
 */
@ApplicationScoped
public class PostgreSqlAdministrativeDivisionRepository implements AdministrativeDivisionRepository {

    private static final String SELECT_DIVISION = """
            SELECT id,
                   country_id,
                   division_type_id,
                   parent_division_id,
                   canonical_code,
                   default_name,
                   official_name,
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
              FROM administrative_divisions
            """;

    private static final String FIND_BY_ID = SELECT_DIVISION + " WHERE id = $1";
    private static final String FIND_BY_CANONICAL_CODE = SELECT_DIVISION
            + " WHERE country_id = $1 AND canonical_code = $2";
    private static final String FIND_BY_COUNTRY = SELECT_DIVISION
            + " WHERE country_id = $1 ORDER BY canonical_code";
    private static final String FIND_BY_PARENT = SELECT_DIVISION
            + """
               WHERE country_id = $1
                 AND parent_division_id IS NOT DISTINCT FROM $2::uuid
               ORDER BY canonical_code
              """;
    private static final String FIND_BY_TYPE_AND_STATUS = SELECT_DIVISION
            + """
               WHERE country_id = $1
                 AND division_type_id = $2
                 AND status = $3::geographic_record_status
               ORDER BY canonical_code
              """;

    private static final String FIND_IDENTIFIERS_BY_DIVISION = """
            SELECT id,
                   country_id,
                   division_id,
                   scheme_code,
                   identifier_value,
                   is_primary,
                   status::text AS status,
                   valid_from,
                   valid_until,
                   source_authority,
                   source_reference,
                   created_at,
                   created_by,
                   updated_at,
                   updated_by,
                   version
              FROM administrative_division_identifiers
             WHERE country_id = $1
               AND division_id = $2
             ORDER BY scheme_code, is_primary DESC, identifier_value, id
            """;

    private static final String FIND_NAMES_BY_DIVISION = """
            SELECT id,
                   country_id,
                   division_id,
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
              FROM administrative_division_names
             WHERE country_id = $1
               AND division_id = $2
             ORDER BY language_tag, name_type, name, id
            """;

    private final Pool pool;
    private final AdministrativeDivisionRowMapper rowMapper;
    private final ReactiveRowSetMapper rowSetMapper;

    public PostgreSqlAdministrativeDivisionRepository(
            final Pool pool,
            final AdministrativeDivisionRowMapper rowMapper,
            final ReactiveRowSetMapper rowSetMapper
    ) {
        this.pool = Objects.requireNonNull(pool, "Pool must not be null");
        this.rowMapper = Objects.requireNonNull(rowMapper, "AdministrativeDivisionRowMapper must not be null");
        this.rowSetMapper = Objects.requireNonNull(rowSetMapper, "ReactiveRowSetMapper must not be null");
    }

    @Override
    public Uni<AdministrativeDivision> findById(final DivisionId id) {
        return pool.preparedQuery(FIND_BY_ID)
                .execute(Tuple.of(id.value()))
                .onItem().transform(rows -> rowSetMapper.firstOrNull(
                        rows,
                        rowMapper::toAdministrativeDivision
                ));
    }

    @Override
    public Uni<AdministrativeDivision> findByCanonicalCode(
            final CountryId countryId,
            final String canonicalCode
    ) {
        return pool.preparedQuery(FIND_BY_CANONICAL_CODE)
                .execute(Tuple.of(countryId.value(), canonicalCode))
                .onItem().transform(rows -> rowSetMapper.firstOrNull(
                        rows,
                        rowMapper::toAdministrativeDivision
                ));
    }

    @Override
    public Uni<List<AdministrativeDivision>> findByCountryId(final CountryId countryId) {
        return pool.preparedQuery(FIND_BY_COUNTRY)
                .execute(Tuple.of(countryId.value()))
                .onItem().transform(rows -> rowSetMapper.toList(
                        rows,
                        rowMapper::toAdministrativeDivision
                ));
    }

    @Override
    public Uni<List<AdministrativeDivision>> findByParentDivisionId(
            final CountryId countryId,
            final DivisionId parentId
    ) {
        return pool.preparedQuery(FIND_BY_PARENT)
                .execute(Tuple.of(countryId.value(), parentId == null ? null : parentId.value()))
                .onItem().transform(rows -> rowSetMapper.toList(
                        rows,
                        rowMapper::toAdministrativeDivision
                ));
    }

    @Override
    public Uni<List<AdministrativeDivision>> findByTypeAndStatus(
            final CountryId countryId,
            final DivisionTypeId typeId,
            final GeographicRecordStatus status
    ) {
        return pool.preparedQuery(FIND_BY_TYPE_AND_STATUS)
                .execute(Tuple.of(countryId.value(), typeId.value(), status.name()))
                .onItem().transform(rows -> rowSetMapper.toList(
                        rows,
                        rowMapper::toAdministrativeDivision
                ));
    }

    @Override
    public Uni<List<AdministrativeDivisionIdentifier>> findIdentifiersByDivisionId(
            final CountryId countryId,
            final DivisionId divisionId
    ) {
        return pool.preparedQuery(FIND_IDENTIFIERS_BY_DIVISION)
                .execute(Tuple.of(countryId.value(), divisionId.value()))
                .onItem().transform(rows -> rowSetMapper.toList(rows, rowMapper::toIdentifier));
    }

    @Override
    public Uni<List<AdministrativeDivisionName>> findNamesByDivisionId(
            final CountryId countryId,
            final DivisionId divisionId
    ) {
        return pool.preparedQuery(FIND_NAMES_BY_DIVISION)
                .execute(Tuple.of(countryId.value(), divisionId.value()))
                .onItem().transform(rows -> rowSetMapper.toList(rows, rowMapper::toName));
    }
}

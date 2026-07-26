package com.alexastudillo.geographicreference.infrastructure.persistence.postgresql;

import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivision;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivisionIdentifier;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivisionName;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicIdentifierStatus;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicNameType;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.AuditInfo;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionTypeId;
import com.alexastudillo.geographicreference.domain.model.valobj.LanguageTag;
import com.alexastudillo.geographicreference.domain.model.valobj.SourceProvenance;
import com.alexastudillo.geographicreference.domain.model.valobj.ValidityPeriod;
import io.vertx.mutiny.sqlclient.Row;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

/**
 * Maps PostgreSQL administrative-division rows into domain objects.
 */
@ApplicationScoped
public class AdministrativeDivisionRowMapper {

    private static final String COUNTRY_ID = "country_id";

    public AdministrativeDivision toAdministrativeDivision(final Row row) {
        final UUID parentId = row.getUUID("parent_division_id");
        return new AdministrativeDivision(
                DivisionId.of(row.getUUID("id")),
                CountryId.of(row.getUUID(COUNTRY_ID)),
                DivisionTypeId.of(row.getUUID("division_type_id")),
                parentId == null ? null : DivisionId.of(parentId),
                row.getString("canonical_code"),
                row.getString("default_name"),
                row.getString("official_name"),
                GeographicRecordStatus.valueOf(row.getString("status")),
                validityPeriod(row),
                sourceProvenance(row, true),
                auditInfo(row));
    }

    public AdministrativeDivisionIdentifier toIdentifier(final Row row) {
        return new AdministrativeDivisionIdentifier(
                row.getUUID("id"),
                CountryId.of(row.getUUID(COUNTRY_ID)),
                DivisionId.of(row.getUUID("division_id")),
                row.getString("scheme_code"),
                row.getString("identifier_value"),
                row.getBoolean("is_primary"),
                GeographicIdentifierStatus.valueOf(row.getString("status")),
                validityPeriod(row),
                sourceProvenance(row, false),
                auditInfo(row));
    }

    public AdministrativeDivisionName toName(final Row row) {
        return new AdministrativeDivisionName(
                row.getUUID("id"),
                CountryId.of(row.getUUID(COUNTRY_ID)),
                DivisionId.of(row.getUUID("division_id")),
                LanguageTag.of(row.getString("language_tag")),
                GeographicNameType.valueOf(row.getString("name_type")),
                row.getString("name"),
                row.getBoolean("is_preferred"),
                validityPeriod(row),
                auditInfo(row));
    }

    private static ValidityPeriod validityPeriod(final Row row) {
        return ValidityPeriod.of(row.getLocalDate("valid_from"), row.getLocalDate("valid_until"));
    }

    private static SourceProvenance sourceProvenance(final Row row, final boolean hasRevision) {
        return SourceProvenance.of(
                row.getString("source_authority"),
                row.getString("source_reference"),
                hasRevision ? row.getString("source_revision") : null);
    }

    private static AuditInfo auditInfo(final Row row) {
        return new AuditInfo(
                row.getOffsetDateTime("created_at"),
                row.getString("created_by"),
                row.getOffsetDateTime("updated_at"),
                row.getString("updated_by"),
                row.getLong("version"));
    }
}

package com.alexastudillo.geographicreference.infrastructure.persistence.postgresql;

import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivisionType;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.AuditInfo;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionTypeId;
import io.vertx.mutiny.sqlclient.Row;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Maps PostgreSQL administrative-division-type rows into domain objects.
 */
@ApplicationScoped
public class AdministrativeDivisionTypeRowMapper {

    public AdministrativeDivisionType toAdministrativeDivisionType(final Row row) {
        return new AdministrativeDivisionType(
                DivisionTypeId.of(row.getUUID("id")),
                CountryId.of(row.getUUID("country_id")),
                row.getString("code"),
                row.getString("name"),
                row.getShort("hierarchy_level"),
                GeographicRecordStatus.valueOf(row.getString("status")),
                new AuditInfo(
                        row.getOffsetDateTime("created_at"),
                        row.getString("created_by"),
                        row.getOffsetDateTime("updated_at"),
                        row.getString("updated_by"),
                        row.getLong("version")
                )
        );
    }
}

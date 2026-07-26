package com.alexastudillo.geographicreference.infrastructure.persistence.postgresql;

import com.alexastudillo.geographicreference.domain.model.entity.Country;
import com.alexastudillo.geographicreference.domain.model.entity.CountryName;
import com.alexastudillo.geographicreference.domain.model.enums.CountryCodeType;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicNameType;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.Alpha2Code;
import com.alexastudillo.geographicreference.domain.model.valobj.Alpha3Code;
import com.alexastudillo.geographicreference.domain.model.valobj.AuditInfo;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.LanguageTag;
import com.alexastudillo.geographicreference.domain.model.valobj.NumericCode;
import com.alexastudillo.geographicreference.domain.model.valobj.SourceProvenance;
import com.alexastudillo.geographicreference.domain.model.valobj.ValidityPeriod;
import com.alexastudillo.geographicreference.domain.model.projection.CountryNameLookup;
import io.vertx.mutiny.sqlclient.Row;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Maps PostgreSQL country rows into framework-independent domain objects.
 */
@ApplicationScoped
public class CountryRowMapper {

    public Country toCountry(final Row row) {
        return new Country(
                CountryId.of(row.getUUID("id")),
                Alpha2Code.of(row.getString("alpha2_code").trim()),
                Alpha3Code.of(row.getString("alpha3_code").trim()),
                NumericCode.of(row.getString("numeric_code").trim()),
                row.getString("default_name"),
                row.getString("official_name"),
                row.getBoolean("is_independent"),
                GeographicRecordStatus.valueOf(row.getString("status")),
                validityPeriod(row),
                sourceProvenance(row),
                auditInfo(row)
        );
    }

    public CountryName toCountryName(final Row row) {
        return new CountryName(
                row.getUUID("id"),
                CountryId.of(row.getUUID("country_id")),
                LanguageTag.of(row.getString("language_tag")),
                GeographicNameType.valueOf(row.getString("name_type")),
                row.getString("name"),
                row.getBoolean("is_preferred"),
                validityPeriod(row),
                auditInfo(row)
        );
    }

    public CountryNameLookup toCountryNameLookup(final Row row, final CountryCodeType codeType) {
        return new CountryNameLookup(
                codeType,
                row.getString("code").trim(),
                LanguageTag.of(row.getString("language_tag")),
                GeographicNameType.valueOf(row.getString("name_type")),
                row.getString("name"),
                row.getBoolean("is_preferred")
        );
    }

    private static ValidityPeriod validityPeriod(final Row row) {
        return ValidityPeriod.of(row.getLocalDate("valid_from"), row.getLocalDate("valid_until"));
    }

    private static SourceProvenance sourceProvenance(final Row row) {
        return SourceProvenance.of(
                row.getString("source_authority"),
                row.getString("source_reference"),
                row.getString("source_revision")
        );
    }

    private static AuditInfo auditInfo(final Row row) {
        return new AuditInfo(
                row.getOffsetDateTime("created_at"),
                row.getString("created_by"),
                row.getOffsetDateTime("updated_at"),
                row.getString("updated_by"),
                row.getLong("version")
        );
    }
}

package com.alexastudillo.geographicreference.domain.model.entity;

import com.alexastudillo.geographicreference.domain.exception.DomainException;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.Alpha2Code;
import com.alexastudillo.geographicreference.domain.model.valobj.Alpha3Code;
import com.alexastudillo.geographicreference.domain.model.valobj.AuditInfo;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.NumericCode;
import com.alexastudillo.geographicreference.domain.model.valobj.SourceProvenance;
import com.alexastudillo.geographicreference.domain.model.valobj.ValidityPeriod;

import java.util.Objects;

/**
 * Aggregate root representing a country or ISO-recognized territory.
 *
 * <p>ISO codes ({@link Alpha2Code}, {@link Alpha3Code}, {@link NumericCode}) serve
 * as stable integration identifiers. Equality and hash code are based exclusively
 * on {@link #id}.
 */
public record Country(
        CountryId id,
        Alpha2Code alpha2Code,
        Alpha3Code alpha3Code,
        NumericCode numericCode,
        String defaultName,
        String officialName,
        boolean independent,
        GeographicRecordStatus status,
        ValidityPeriod validityPeriod,
        SourceProvenance sourceProvenance,
        AuditInfo auditInfo
) {

    public Country {
        Objects.requireNonNull(id, "Country id must not be null");
        Objects.requireNonNull(alpha2Code, "Alpha-2 code must not be null");
        Objects.requireNonNull(alpha3Code, "Alpha-3 code must not be null");
        Objects.requireNonNull(numericCode, "Numeric code must not be null");
        Objects.requireNonNull(defaultName, "Default name must not be null");
        if (defaultName.isBlank()) {
            throw new DomainException("Default name must not be blank");
        }
        Objects.requireNonNull(officialName, "Official name must not be null");
        if (officialName.isBlank()) {
            throw new DomainException("Official name must not be blank");
        }
        Objects.requireNonNull(status, "Status must not be null");
        Objects.requireNonNull(validityPeriod, "Validity period must not be null");
        Objects.requireNonNull(sourceProvenance, "Source provenance must not be null");
        Objects.requireNonNull(auditInfo, "Audit info must not be null");
    }

    // ── Identity-based equality ────────────────────────────────────────────

    @Override
    public boolean equals(final Object o) {
        return this == o || (o instanceof Country that && this.id.equals(that.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

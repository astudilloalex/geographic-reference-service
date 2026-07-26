package com.alexastudillo.geographicreference.domain.model.entity;

import com.alexastudillo.geographicreference.domain.exception.DomainException;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicNameType;
import com.alexastudillo.geographicreference.domain.model.valobj.AuditInfo;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionId;
import com.alexastudillo.geographicreference.domain.model.valobj.LanguageTag;
import com.alexastudillo.geographicreference.domain.model.valobj.ValidityPeriod;

import java.util.Objects;
import java.util.UUID;

/**
 * Localized, alternative or historical name for an {@link AdministrativeDivision}.
 *
 * <p>At most one {@code preferred} name per division and language tag is
 * allowed (enforced via a partial unique index in the database).
 */
public record AdministrativeDivisionName(
        UUID id,
        CountryId countryId,
        DivisionId divisionId,
        LanguageTag languageTag,
        GeographicNameType nameType,
        String name,
        boolean preferred,
        ValidityPeriod validityPeriod,
        AuditInfo auditInfo
) {

    public AdministrativeDivisionName {
        Objects.requireNonNull(id, "DivisionName id must not be null");
        Objects.requireNonNull(countryId, "Country id must not be null");
        Objects.requireNonNull(divisionId, "Division id must not be null");
        Objects.requireNonNull(languageTag, "Language tag must not be null");
        Objects.requireNonNull(nameType, "Name type must not be null");
        Objects.requireNonNull(name, "Name must not be null");
        if (name.isBlank()) {
            throw new DomainException("Division name must not be blank");
        }
        Objects.requireNonNull(validityPeriod, "Validity period must not be null");
        Objects.requireNonNull(auditInfo, "Audit info must not be null");
    }

    // ── Identity-based equality ────────────────────────────────────────────

    @Override
    public boolean equals(final Object o) {
        return this == o || (o instanceof AdministrativeDivisionName that && this.id.equals(that.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

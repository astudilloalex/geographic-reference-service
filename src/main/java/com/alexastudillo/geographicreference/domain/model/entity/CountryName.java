package com.alexastudillo.geographicreference.domain.model.entity;

import com.alexastudillo.geographicreference.domain.exception.DomainException;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicNameType;
import com.alexastudillo.geographicreference.domain.model.valobj.AuditInfo;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.LanguageTag;
import com.alexastudillo.geographicreference.domain.model.valobj.ValidityPeriod;

import java.util.Objects;
import java.util.UUID;

/**
 * Localized, alternative or historical name for a {@link Country}.
 *
 * <p>Identity is based on the surrogate {@code id}; the natural uniqueness
 * constraint {@code (countryId, languageTag, nameType, name)} is enforced
 * at the persistence layer.
 */
public record CountryName(
        UUID id,
        CountryId countryId,
        LanguageTag languageTag,
        GeographicNameType nameType,
        String name,
        boolean preferred,
        ValidityPeriod validityPeriod,
        AuditInfo auditInfo
) {

    public CountryName {
        Objects.requireNonNull(id, "CountryName id must not be null");
        Objects.requireNonNull(countryId, "Country id must not be null");
        Objects.requireNonNull(languageTag, "Language tag must not be null");
        Objects.requireNonNull(nameType, "Name type must not be null");
        Objects.requireNonNull(name, "Name must not be null");
        if (name.isBlank()) {
            throw new DomainException("Country name must not be blank");
        }
        Objects.requireNonNull(validityPeriod, "Validity period must not be null");
        Objects.requireNonNull(auditInfo, "Audit info must not be null");
    }

    // ── Identity-based equality ────────────────────────────────────────────

    @Override
    public boolean equals(final Object o) {
        return this == o || (o instanceof CountryName that && this.id.equals(that.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

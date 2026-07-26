package com.alexastudillo.geographicreference.domain.model.entity;

import com.alexastudillo.geographicreference.domain.exception.DomainException;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicIdentifierStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.AuditInfo;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionId;
import com.alexastudillo.geographicreference.domain.model.valobj.SourceProvenance;
import com.alexastudillo.geographicreference.domain.model.valobj.ValidityPeriod;

import java.util.Objects;
import java.util.UUID;

/**
 * External identifier assigned to an {@link AdministrativeDivision} under a
 * specific scheme (e.g. {@code ISO_3166_2}, {@code EC_INEC_DPA}).
 *
 * <p>A division may carry identifiers from multiple schemes simultaneously.
 * At most one {@code ACTIVE} identifier per scheme may be flagged as
 * {@code isPrimary} (enforced via a partial unique index in the database).
 */
public record AdministrativeDivisionIdentifier(
        UUID id,
        CountryId countryId,
        DivisionId divisionId,
        String schemeCode,
        String identifierValue,
        boolean primary,
        GeographicIdentifierStatus status,
        ValidityPeriod validityPeriod,
        SourceProvenance sourceProvenance,
        AuditInfo auditInfo
) {

    public AdministrativeDivisionIdentifier {
        Objects.requireNonNull(id, "Identifier id must not be null");
        Objects.requireNonNull(countryId, "Country id must not be null");
        Objects.requireNonNull(divisionId, "Division id must not be null");
        Objects.requireNonNull(schemeCode, "Scheme code must not be null");
        if (schemeCode.isBlank()) {
            throw new DomainException("Scheme code must not be blank");
        }
        Objects.requireNonNull(identifierValue, "Identifier value must not be null");
        if (identifierValue.isBlank()) {
            throw new DomainException("Identifier value must not be blank");
        }
        Objects.requireNonNull(status, "Status must not be null");
        Objects.requireNonNull(validityPeriod, "Validity period must not be null");
        Objects.requireNonNull(sourceProvenance, "Source provenance must not be null");
        Objects.requireNonNull(auditInfo, "Audit info must not be null");
    }

    // ── Identity-based equality ────────────────────────────────────────────

    @Override
    public boolean equals(final Object o) {
        return this == o || (o instanceof AdministrativeDivisionIdentifier that && this.id.equals(that.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

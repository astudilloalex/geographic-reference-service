package com.alexastudillo.geographicreference.domain.model.entity;

import com.alexastudillo.geographicreference.domain.exception.DomainException;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.AuditInfo;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionTypeId;
import com.alexastudillo.geographicreference.domain.model.valobj.SourceProvenance;
import com.alexastudillo.geographicreference.domain.model.valobj.ValidityPeriod;

import java.util.Objects;

/**
 * Aggregate root representing a hierarchical political-administrative division
 * within a country (e.g. Province, Canton, Parish).
 *
 * <p>{@code parentDivisionId} is {@code null} for root divisions. Hierarchy
 * consistency (same country, consecutive levels, no cycles) is enforced by a
 * deferred constraint trigger at the database level.
 */
public record AdministrativeDivision(
        DivisionId id,
        CountryId countryId,
        DivisionTypeId divisionTypeId,
        DivisionId parentDivisionId,
        String canonicalCode,
        String defaultName,
        String officialName,
        GeographicRecordStatus status,
        ValidityPeriod validityPeriod,
        SourceProvenance sourceProvenance,
        AuditInfo auditInfo
) {

    public AdministrativeDivision {
        Objects.requireNonNull(id, "Division id must not be null");
        Objects.requireNonNull(countryId, "Country id must not be null");
        Objects.requireNonNull(divisionTypeId, "Division type id must not be null");
        // parentDivisionId may be null (root division)
        Objects.requireNonNull(canonicalCode, "Canonical code must not be null");
        if (canonicalCode.isBlank()) {
            throw new DomainException("Canonical code must not be blank");
        }
        Objects.requireNonNull(defaultName, "Default name must not be null");
        if (defaultName.isBlank()) {
            throw new DomainException("Default name must not be blank");
        }
        // officialName may be null
        Objects.requireNonNull(status, "Status must not be null");
        Objects.requireNonNull(validityPeriod, "Validity period must not be null");
        Objects.requireNonNull(sourceProvenance, "Source provenance must not be null");
        Objects.requireNonNull(auditInfo, "Audit info must not be null");

        if (parentDivisionId != null && parentDivisionId.equals(id)) {
            throw new DomainException("A division cannot be its own parent");
        }
    }

    /**
     * Returns {@code true} when this division sits at the top of the hierarchy.
     */
    public boolean isRoot() {
        return parentDivisionId == null;
    }

    // ── Identity-based equality ────────────────────────────────────────────

    @Override
    public boolean equals(final Object o) {
        return this == o || (o instanceof AdministrativeDivision that && this.id.equals(that.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

package com.alexastudillo.geographicreference.domain.model.entity;

import com.alexastudillo.geographicreference.domain.exception.DomainException;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.AuditInfo;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionTypeId;

import java.util.Objects;

/**
 * Country-specific administrative level definition (e.g. Province, Canton, Parish).
 *
 * <p>{@code hierarchyLevel} is one-based; lower values are higher in the
 * political-administrative tree. The combination {@code (countryId, code)} and
 * {@code (countryId, hierarchyLevel)} are unique (enforced at the persistence layer).
 */
public record AdministrativeDivisionType(
        DivisionTypeId id,
        CountryId countryId,
        String code,
        String name,
        short hierarchyLevel,
        GeographicRecordStatus status,
        AuditInfo auditInfo
) {

    public AdministrativeDivisionType {
        Objects.requireNonNull(id, "DivisionType id must not be null");
        Objects.requireNonNull(countryId, "Country id must not be null");
        Objects.requireNonNull(code, "Code must not be null");
        if (code.isBlank()) {
            throw new DomainException("Division type code must not be blank");
        }
        Objects.requireNonNull(name, "Name must not be null");
        if (name.isBlank()) {
            throw new DomainException("Division type name must not be blank");
        }
        if (hierarchyLevel <= 0) {
            throw new DomainException(
                    "Hierarchy level must be greater than zero, got: %d".formatted(hierarchyLevel)
            );
        }
        Objects.requireNonNull(status, "Status must not be null");
        Objects.requireNonNull(auditInfo, "Audit info must not be null");
    }

    // ── Identity-based equality ────────────────────────────────────────────

    @Override
    public boolean equals(final Object o) {
        return this == o || (o instanceof AdministrativeDivisionType that && this.id.equals(that.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

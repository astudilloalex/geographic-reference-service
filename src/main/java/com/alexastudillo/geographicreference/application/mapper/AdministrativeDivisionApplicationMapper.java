package com.alexastudillo.geographicreference.application.mapper;

import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionIdentifierResponse;
import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionNameResponse;
import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionResponse;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivision;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivisionIdentifier;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivisionName;

/**
 * Pure Java mapper for converting AdministrativeDivision domain entities to DTO responses.
 */
public final class AdministrativeDivisionApplicationMapper {

    private AdministrativeDivisionApplicationMapper() {
        // Utility class
    }

    public static AdministrativeDivisionResponse toResponse(final AdministrativeDivision division) {
        if (division == null) {
            return null;
        }
        return new AdministrativeDivisionResponse(
                division.id().value(),
                division.countryId().value(),
                division.divisionTypeId().value(),
                division.parentDivisionId() != null ? division.parentDivisionId().value() : null,
                division.canonicalCode(),
                division.defaultName(),
                division.officialName(),
                division.status().name(),
                division.validityPeriod().validFrom(),
                division.validityPeriod().validUntil(),
                division.sourceProvenance().authority(),
                division.sourceProvenance().reference(),
                division.sourceProvenance().revision(),
                division.auditInfo().createdAt(),
                division.auditInfo().createdBy(),
                division.auditInfo().updatedAt(),
                division.auditInfo().updatedBy(),
                division.auditInfo().version()
        );
    }

    public static AdministrativeDivisionIdentifierResponse toIdentifierResponse(final AdministrativeDivisionIdentifier identifier) {
        if (identifier == null) {
            return null;
        }
        return new AdministrativeDivisionIdentifierResponse(
                identifier.id(),
                identifier.countryId().value(),
                identifier.divisionId().value(),
                identifier.schemeCode(),
                identifier.identifierValue(),
                identifier.primary(),
                identifier.status().name(),
                identifier.validityPeriod().validFrom(),
                identifier.validityPeriod().validUntil(),
                identifier.sourceProvenance().authority(),
                identifier.sourceProvenance().reference(),
                identifier.auditInfo().createdAt(),
                identifier.auditInfo().createdBy(),
                identifier.auditInfo().updatedAt(),
                identifier.auditInfo().updatedBy(),
                identifier.auditInfo().version()
        );
    }

    public static AdministrativeDivisionNameResponse toNameResponse(final AdministrativeDivisionName name) {
        if (name == null) {
            return null;
        }
        return new AdministrativeDivisionNameResponse(
                name.id(),
                name.countryId().value(),
                name.divisionId().value(),
                name.languageTag().value(),
                name.nameType().name(),
                name.name(),
                name.preferred(),
                name.validityPeriod().validFrom(),
                name.validityPeriod().validUntil(),
                name.auditInfo().createdAt(),
                name.auditInfo().createdBy(),
                name.auditInfo().updatedAt(),
                name.auditInfo().updatedBy(),
                name.auditInfo().version()
        );
    }
}

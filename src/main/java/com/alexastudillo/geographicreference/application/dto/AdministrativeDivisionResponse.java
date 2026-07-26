package com.alexastudillo.geographicreference.application.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Data transfer object representing administrative division query response.
 */
public record AdministrativeDivisionResponse(
        UUID id,
        UUID countryId,
        UUID divisionTypeId,
        UUID parentDivisionId,
        String canonicalCode,
        String defaultName,
        String officialName,
        String status,
        LocalDate validFrom,
        LocalDate validUntil,
        String sourceAuthority,
        String sourceReference,
        String sourceRevision,
        OffsetDateTime createdAt,
        String createdBy,
        OffsetDateTime updatedAt,
        String updatedBy,
        long version
) {
}

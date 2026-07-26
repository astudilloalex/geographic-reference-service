package com.alexastudillo.geographicreference.application.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Data transfer object representing administrative division identifier query response.
 */
public record AdministrativeDivisionIdentifierResponse(
        UUID id,
        UUID countryId,
        UUID divisionId,
        String schemeCode,
        String identifierValue,
        boolean primary,
        String status,
        LocalDate validFrom,
        LocalDate validUntil,
        String sourceAuthority,
        String sourceReference,
        OffsetDateTime createdAt,
        String createdBy,
        OffsetDateTime updatedAt,
        String updatedBy,
        long version
) {
}

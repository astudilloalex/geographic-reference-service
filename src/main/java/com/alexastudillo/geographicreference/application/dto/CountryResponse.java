package com.alexastudillo.geographicreference.application.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Data transfer object representing country query response.
 */
public record CountryResponse(
        UUID id,
        String alpha2Code,
        String alpha3Code,
        String numericCode,
        String defaultName,
        String officialName,
        boolean independent,
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

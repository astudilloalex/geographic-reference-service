package com.alexastudillo.geographicreference.infrastructure.api.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@RegisterForReflection
public record AdministrativeDivisionApiResponse(
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

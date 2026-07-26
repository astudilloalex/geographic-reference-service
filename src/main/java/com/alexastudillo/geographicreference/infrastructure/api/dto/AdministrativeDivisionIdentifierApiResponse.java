package com.alexastudillo.geographicreference.infrastructure.api.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@RegisterForReflection
public record AdministrativeDivisionIdentifierApiResponse(
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

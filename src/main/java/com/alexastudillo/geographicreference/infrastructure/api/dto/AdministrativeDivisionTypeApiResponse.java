package com.alexastudillo.geographicreference.infrastructure.api.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.OffsetDateTime;
import java.util.UUID;

@RegisterForReflection
public record AdministrativeDivisionTypeApiResponse(
        UUID id,
        UUID countryId,
        String code,
        String name,
        short hierarchyLevel,
        String status,
        OffsetDateTime createdAt,
        String createdBy,
        OffsetDateTime updatedAt,
        String updatedBy,
        long version
) {
}

package com.alexastudillo.geographicreference.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Data transfer object representing administrative division type query response.
 */
public record AdministrativeDivisionTypeResponse(
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

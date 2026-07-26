package com.alexastudillo.geographicreference.application.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Data transfer object representing country name query response.
 */
public record CountryNameResponse(
        UUID id,
        UUID countryId,
        String languageTag,
        String nameType,
        String name,
        boolean preferred,
        LocalDate validFrom,
        LocalDate validUntil,
        OffsetDateTime createdAt,
        String createdBy,
        OffsetDateTime updatedAt,
        String updatedBy,
        long version
) {
}

package com.alexastudillo.geographicreference.infrastructure.api.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@RegisterForReflection
public record CountryNameApiResponse(
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

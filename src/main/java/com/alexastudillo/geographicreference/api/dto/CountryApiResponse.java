package com.alexastudillo.geographicreference.api.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@RegisterForReflection
public record CountryApiResponse(
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

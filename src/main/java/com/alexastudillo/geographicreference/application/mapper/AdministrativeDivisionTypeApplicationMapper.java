package com.alexastudillo.geographicreference.application.mapper;

import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionTypeResponse;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivisionType;

/**
 * Pure Java mapper for converting AdministrativeDivisionType domain entities to DTO responses.
 */
public final class AdministrativeDivisionTypeApplicationMapper {

    private AdministrativeDivisionTypeApplicationMapper() {
        // Utility class
    }

    public static AdministrativeDivisionTypeResponse toResponse(final AdministrativeDivisionType type) {
        if (type == null) {
            return null;
        }
        return new AdministrativeDivisionTypeResponse(
                type.id().value(),
                type.countryId().value(),
                type.code(),
                type.name(),
                type.hierarchyLevel(),
                type.status().name(),
                type.auditInfo().createdAt(),
                type.auditInfo().createdBy(),
                type.auditInfo().updatedAt(),
                type.auditInfo().updatedBy(),
                type.auditInfo().version()
        );
    }
}

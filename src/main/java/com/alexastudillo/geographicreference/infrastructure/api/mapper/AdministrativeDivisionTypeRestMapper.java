package com.alexastudillo.geographicreference.infrastructure.api.mapper;

import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionTypeResponse;
import com.alexastudillo.geographicreference.infrastructure.api.dto.AdministrativeDivisionTypeApiResponse;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AdministrativeDivisionTypeRestMapper {

    public AdministrativeDivisionTypeApiResponse toApiResponse(final AdministrativeDivisionTypeResponse source) {
        return new AdministrativeDivisionTypeApiResponse(
                source.id(),
                source.countryId(),
                source.code(),
                source.name(),
                source.hierarchyLevel(),
                source.status(),
                source.createdAt(),
                source.createdBy(),
                source.updatedAt(),
                source.updatedBy(),
                source.version()
        );
    }
}

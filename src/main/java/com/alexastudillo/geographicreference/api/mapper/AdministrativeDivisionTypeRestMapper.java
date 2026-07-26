package com.alexastudillo.geographicreference.api.mapper;

import com.alexastudillo.geographicreference.api.dto.AdministrativeDivisionTypeApiResponse;
import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionTypeResponse;
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

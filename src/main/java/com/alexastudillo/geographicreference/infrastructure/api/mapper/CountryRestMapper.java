package com.alexastudillo.geographicreference.infrastructure.api.mapper;

import com.alexastudillo.geographicreference.application.dto.CountryNameResponse;
import com.alexastudillo.geographicreference.application.dto.CountryResponse;
import com.alexastudillo.geographicreference.infrastructure.api.dto.CountryApiResponse;
import com.alexastudillo.geographicreference.infrastructure.api.dto.CountryNameApiResponse;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CountryRestMapper {

    public CountryApiResponse toApiResponse(final CountryResponse source) {
        return new CountryApiResponse(
                source.id(),
                source.alpha2Code(),
                source.alpha3Code(),
                source.numericCode(),
                source.defaultName(),
                source.officialName(),
                source.independent(),
                source.status(),
                source.validFrom(),
                source.validUntil(),
                source.sourceAuthority(),
                source.sourceReference(),
                source.sourceRevision(),
                source.createdAt(),
                source.createdBy(),
                source.updatedAt(),
                source.updatedBy(),
                source.version()
        );
    }

    public CountryNameApiResponse toNameApiResponse(final CountryNameResponse source) {
        return new CountryNameApiResponse(
                source.id(),
                source.countryId(),
                source.languageTag(),
                source.nameType(),
                source.name(),
                source.preferred(),
                source.validFrom(),
                source.validUntil(),
                source.createdAt(),
                source.createdBy(),
                source.updatedAt(),
                source.updatedBy(),
                source.version()
        );
    }
}

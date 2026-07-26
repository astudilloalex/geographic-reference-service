package com.alexastudillo.geographicreference.application.mapper;

import com.alexastudillo.geographicreference.application.dto.CountryNameLookupResponse;
import com.alexastudillo.geographicreference.application.dto.CountryNameResponse;
import com.alexastudillo.geographicreference.application.dto.CountryResponse;
import com.alexastudillo.geographicreference.domain.model.entity.Country;
import com.alexastudillo.geographicreference.domain.model.entity.CountryName;
import com.alexastudillo.geographicreference.domain.model.projection.CountryNameLookup;

/**
 * Pure Java mapper for converting Country domain entities to DTO responses.
 */
public final class CountryApplicationMapper {

    private CountryApplicationMapper() {
        // Utility class
    }

    public static CountryResponse toResponse(final Country country) {
        if (country == null) {
            return null;
        }
        return new CountryResponse(
                country.id().value(),
                country.alpha2Code().value(),
                country.alpha3Code().value(),
                country.numericCode().value(),
                country.defaultName(),
                country.officialName(),
                country.independent(),
                country.status().name(),
                country.validityPeriod().validFrom(),
                country.validityPeriod().validUntil(),
                country.sourceProvenance().authority(),
                country.sourceProvenance().reference(),
                country.sourceProvenance().revision(),
                country.auditInfo().createdAt(),
                country.auditInfo().createdBy(),
                country.auditInfo().updatedAt(),
                country.auditInfo().updatedBy(),
                country.auditInfo().version()
        );
    }

    public static CountryNameResponse toNameResponse(final CountryName countryName) {
        if (countryName == null) {
            return null;
        }
        return new CountryNameResponse(
                countryName.id(),
                countryName.countryId().value(),
                countryName.languageTag().value(),
                countryName.nameType().name(),
                countryName.name(),
                countryName.preferred(),
                countryName.validityPeriod().validFrom(),
                countryName.validityPeriod().validUntil(),
                countryName.auditInfo().createdAt(),
                countryName.auditInfo().createdBy(),
                countryName.auditInfo().updatedAt(),
                countryName.auditInfo().updatedBy(),
                countryName.auditInfo().version()
        );
    }

    public static CountryNameLookupResponse toNameLookupResponse(final CountryNameLookup countryName) {
        if (countryName == null) {
            return null;
        }
        return new CountryNameLookupResponse(
                countryName.codeType().name(),
                countryName.code(),
                countryName.languageTag().value(),
                countryName.nameType().name(),
                countryName.name(),
                countryName.preferred()
        );
    }
}

package com.alexastudillo.geographicreference.api.mapper;

import com.alexastudillo.geographicreference.api.dto.AdministrativeDivisionApiResponse;
import com.alexastudillo.geographicreference.api.dto.AdministrativeDivisionIdentifierApiResponse;
import com.alexastudillo.geographicreference.api.dto.AdministrativeDivisionNameApiResponse;
import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionIdentifierResponse;
import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionNameResponse;
import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionResponse;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AdministrativeDivisionRestMapper {

    public AdministrativeDivisionApiResponse toApiResponse(final AdministrativeDivisionResponse source) {
        return new AdministrativeDivisionApiResponse(
                source.id(),
                source.countryId(),
                source.divisionTypeId(),
                source.parentDivisionId(),
                source.canonicalCode(),
                source.defaultName(),
                source.officialName(),
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

    public AdministrativeDivisionIdentifierApiResponse toIdentifierApiResponse(
            final AdministrativeDivisionIdentifierResponse source
    ) {
        return new AdministrativeDivisionIdentifierApiResponse(
                source.id(),
                source.countryId(),
                source.divisionId(),
                source.schemeCode(),
                source.identifierValue(),
                source.primary(),
                source.status(),
                source.validFrom(),
                source.validUntil(),
                source.sourceAuthority(),
                source.sourceReference(),
                source.createdAt(),
                source.createdBy(),
                source.updatedAt(),
                source.updatedBy(),
                source.version()
        );
    }

    public AdministrativeDivisionNameApiResponse toNameApiResponse(
            final AdministrativeDivisionNameResponse source
    ) {
        return new AdministrativeDivisionNameApiResponse(
                source.id(),
                source.countryId(),
                source.divisionId(),
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

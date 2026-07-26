package com.alexastudillo.geographicreference.application.service;

import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionTypeResponse;
import com.alexastudillo.geographicreference.application.mapper.AdministrativeDivisionTypeApplicationMapper;
import com.alexastudillo.geographicreference.application.port.input.GetAdministrativeDivisionTypeQueryPort;
import com.alexastudillo.geographicreference.application.port.output.AdministrativeDivisionTypeRepository;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionTypeId;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.util.Objects;
import java.util.UUID;

/**
 * Application service implementing administrative division type read queries.
 */
public class GetAdministrativeDivisionTypeQueryService implements GetAdministrativeDivisionTypeQueryPort {

    private final AdministrativeDivisionTypeRepository repository;

    public GetAdministrativeDivisionTypeQueryService(final AdministrativeDivisionTypeRepository repository) {
        this.repository = Objects.requireNonNull(repository, "AdministrativeDivisionTypeRepository must not be null");
    }

    @Override
    public Uni<AdministrativeDivisionTypeResponse> findById(final UUID id) {
        if (id == null) {
            return Uni.createFrom().nullItem();
        }
        return repository.findById(DivisionTypeId.of(id))
                .onItem().ifNotNull().transform(AdministrativeDivisionTypeApplicationMapper::toResponse);
    }

    @Override
    public Uni<AdministrativeDivisionTypeResponse> findByCountryIdAndCode(final UUID countryId,
            final String code) {
        if (countryId == null || code == null || code.isBlank()) {
            return Uni.createFrom().nullItem();
        }
        return repository.findByCountryIdAndCode(CountryId.of(countryId), code.trim().toUpperCase())
                .onItem().ifNotNull().transform(AdministrativeDivisionTypeApplicationMapper::toResponse);
    }

    @Override
    public Multi<AdministrativeDivisionTypeResponse> listByCountryId(final UUID countryId) {
        if (countryId == null) {
            return Multi.createFrom().empty();
        }
        return repository.findByCountryId(CountryId.of(countryId))
                .onItem().transform(AdministrativeDivisionTypeApplicationMapper::toResponse);
    }

    @Override
    public Multi<AdministrativeDivisionTypeResponse> listByCountryIdAndStatus(final UUID countryId,
            final String status) {
        if (countryId == null || status == null || status.isBlank()) {
            return Multi.createFrom().empty();
        }
        try {
            final GeographicRecordStatus recordStatus = GeographicRecordStatus.valueOf(status.trim().toUpperCase());
            return repository.findByCountryIdAndStatus(CountryId.of(countryId), recordStatus)
                    .onItem().transform(AdministrativeDivisionTypeApplicationMapper::toResponse);
        } catch (IllegalArgumentException _) {
            return Multi.createFrom().empty();
        }
    }
}

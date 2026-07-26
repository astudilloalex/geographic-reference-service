package com.alexastudillo.geographicreference.application.service;

import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionTypeResponse;
import com.alexastudillo.geographicreference.application.mapper.AdministrativeDivisionTypeApplicationMapper;
import com.alexastudillo.geographicreference.application.port.input.GetAdministrativeDivisionTypeQueryPort;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionTypeId;
import com.alexastudillo.geographicreference.domain.port.output.AdministrativeDivisionTypeRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
    public Optional<AdministrativeDivisionTypeResponse> findById(final UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return repository.findById(DivisionTypeId.of(id))
                .map(AdministrativeDivisionTypeApplicationMapper::toResponse);
    }

    @Override
    public Optional<AdministrativeDivisionTypeResponse> findByCountryIdAndCode(final UUID countryId,
            final String code) {
        if (countryId == null || code == null || code.isBlank()) {
            return Optional.empty();
        }
        return repository.findByCountryIdAndCode(CountryId.of(countryId), code.trim().toUpperCase())
                .map(AdministrativeDivisionTypeApplicationMapper::toResponse);
    }

    @Override
    public List<AdministrativeDivisionTypeResponse> listByCountryId(final UUID countryId) {
        if (countryId == null) {
            return List.of();
        }
        return repository.findByCountryId(CountryId.of(countryId)).stream()
                .map(AdministrativeDivisionTypeApplicationMapper::toResponse)
                .toList();
    }

    @Override
    public List<AdministrativeDivisionTypeResponse> listByCountryIdAndStatus(final UUID countryId,
            final String status) {
        if (countryId == null || status == null || status.isBlank()) {
            return List.of();
        }
        try {
            final GeographicRecordStatus recordStatus = GeographicRecordStatus.valueOf(status.trim().toUpperCase());
            return repository.findByCountryIdAndStatus(CountryId.of(countryId), recordStatus).stream()
                    .map(AdministrativeDivisionTypeApplicationMapper::toResponse)
                    .toList();
        } catch (IllegalArgumentException _) {
            return List.of();
        }
    }
}

package com.alexastudillo.geographicreference.application.service;

import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionIdentifierResponse;
import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionNameResponse;
import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionResponse;
import com.alexastudillo.geographicreference.application.mapper.AdministrativeDivisionApplicationMapper;
import com.alexastudillo.geographicreference.application.port.input.GetAdministrativeDivisionQueryPort;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionTypeId;
import com.alexastudillo.geographicreference.domain.port.output.AdministrativeDivisionRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service implementing administrative division read queries.
 */
public class GetAdministrativeDivisionQueryService implements GetAdministrativeDivisionQueryPort {

    private final AdministrativeDivisionRepository repository;

    public GetAdministrativeDivisionQueryService(final AdministrativeDivisionRepository repository) {
        this.repository = Objects.requireNonNull(repository, "AdministrativeDivisionRepository must not be null");
    }

    @Override
    public Optional<AdministrativeDivisionResponse> findById(final UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return repository.findById(DivisionId.of(id))
                .map(AdministrativeDivisionApplicationMapper::toResponse);
    }

    @Override
    public Optional<AdministrativeDivisionResponse> findByCanonicalCode(final UUID countryId,
            final String canonicalCode) {
        if (countryId == null || canonicalCode == null || canonicalCode.isBlank()) {
            return Optional.empty();
        }
        return repository.findByCanonicalCode(CountryId.of(countryId), canonicalCode.trim().toUpperCase())
                .map(AdministrativeDivisionApplicationMapper::toResponse);
    }

    @Override
    public List<AdministrativeDivisionResponse> listByCountryId(final UUID countryId) {
        if (countryId == null) {
            return List.of();
        }
        return repository.findByCountryId(CountryId.of(countryId)).stream()
                .map(AdministrativeDivisionApplicationMapper::toResponse)
                .toList();
    }

    @Override
    public List<AdministrativeDivisionResponse> listByParentId(final UUID countryId, final UUID parentId) {
        if (countryId == null) {
            return List.of();
        }
        final DivisionId parentDivisionId = parentId != null ? DivisionId.of(parentId) : null;
        return repository.findByParentDivisionId(CountryId.of(countryId), parentDivisionId).stream()
                .map(AdministrativeDivisionApplicationMapper::toResponse)
                .toList();
    }

    @Override
    public List<AdministrativeDivisionResponse> listByTypeAndStatus(final UUID countryId, final UUID typeId,
            final String status) {
        if (countryId == null || typeId == null || status == null || status.isBlank()) {
            return List.of();
        }
        try {
            final GeographicRecordStatus recordStatus = GeographicRecordStatus.valueOf(status.trim().toUpperCase());
            return repository.findByTypeAndStatus(CountryId.of(countryId), DivisionTypeId.of(typeId), recordStatus)
                    .stream()
                    .map(AdministrativeDivisionApplicationMapper::toResponse)
                    .toList();
        } catch (IllegalArgumentException _) {
            return List.of();
        }
    }

    @Override
    public List<AdministrativeDivisionIdentifierResponse> findIdentifiersByDivisionId(final UUID countryId,
            final UUID divisionId) {
        if (countryId == null || divisionId == null) {
            return List.of();
        }
        return repository.findIdentifiersByDivisionId(CountryId.of(countryId), DivisionId.of(divisionId)).stream()
                .map(AdministrativeDivisionApplicationMapper::toIdentifierResponse)
                .toList();
    }

    @Override
    public List<AdministrativeDivisionNameResponse> findNamesByDivisionId(final UUID countryId, final UUID divisionId) {
        if (countryId == null || divisionId == null) {
            return List.of();
        }
        return repository.findNamesByDivisionId(CountryId.of(countryId), DivisionId.of(divisionId)).stream()
                .map(AdministrativeDivisionApplicationMapper::toNameResponse)
                .toList();
    }
}

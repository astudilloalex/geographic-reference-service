package com.alexastudillo.geographicreference.application.service;

import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionIdentifierResponse;
import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionNameResponse;
import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionResponse;
import com.alexastudillo.geographicreference.application.mapper.AdministrativeDivisionApplicationMapper;
import com.alexastudillo.geographicreference.application.port.input.GetAdministrativeDivisionQueryPort;
import com.alexastudillo.geographicreference.application.port.output.AdministrativeDivisionRepository;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionTypeId;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.Objects;
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
    public Uni<AdministrativeDivisionResponse> findById(final UUID id) {
        if (id == null) {
            return Uni.createFrom().nullItem();
        }
        return repository.findById(DivisionId.of(id))
                .onItem().ifNotNull().transform(AdministrativeDivisionApplicationMapper::toResponse);
    }

    @Override
    public Uni<AdministrativeDivisionResponse> findByCanonicalCode(final UUID countryId,
            final String canonicalCode) {
        if (countryId == null || canonicalCode == null || canonicalCode.isBlank()) {
            return Uni.createFrom().nullItem();
        }
        return repository.findByCanonicalCode(CountryId.of(countryId), canonicalCode.trim().toUpperCase())
                .onItem().ifNotNull().transform(AdministrativeDivisionApplicationMapper::toResponse);
    }

    @Override
    public Uni<List<AdministrativeDivisionResponse>> listByCountryId(final UUID countryId) {
        if (countryId == null) {
            return Uni.createFrom().item(List.of());
        }
        return repository.findByCountryId(CountryId.of(countryId))
                .onItem().transform(divisions -> divisions.stream()
                        .map(AdministrativeDivisionApplicationMapper::toResponse)
                        .toList());
    }

    @Override
    public Uni<List<AdministrativeDivisionResponse>> listByParentId(final UUID countryId, final UUID parentId) {
        if (countryId == null) {
            return Uni.createFrom().item(List.of());
        }
        final DivisionId parentDivisionId = parentId != null ? DivisionId.of(parentId) : null;
        return repository.findByParentDivisionId(CountryId.of(countryId), parentDivisionId)
                .onItem().transform(divisions -> divisions.stream()
                        .map(AdministrativeDivisionApplicationMapper::toResponse)
                        .toList());
    }

    @Override
    public Uni<List<AdministrativeDivisionResponse>> listByTypeAndStatus(final UUID countryId, final UUID typeId,
            final String status) {
        if (countryId == null || typeId == null || status == null || status.isBlank()) {
            return Uni.createFrom().item(List.of());
        }
        try {
            final GeographicRecordStatus recordStatus = GeographicRecordStatus.valueOf(status.trim().toUpperCase());
            return repository.findByTypeAndStatus(CountryId.of(countryId), DivisionTypeId.of(typeId), recordStatus)
                    .onItem().transform(divisions -> divisions.stream()
                            .map(AdministrativeDivisionApplicationMapper::toResponse)
                            .toList());
        } catch (IllegalArgumentException _) {
            return Uni.createFrom().item(List.of());
        }
    }

    @Override
    public Uni<List<AdministrativeDivisionIdentifierResponse>> findIdentifiersByDivisionId(final UUID countryId,
            final UUID divisionId) {
        if (countryId == null || divisionId == null) {
            return Uni.createFrom().item(List.of());
        }
        return repository.findIdentifiersByDivisionId(CountryId.of(countryId), DivisionId.of(divisionId))
                .onItem().transform(identifiers -> identifiers.stream()
                        .map(AdministrativeDivisionApplicationMapper::toIdentifierResponse)
                        .toList());
    }

    @Override
    public Uni<List<AdministrativeDivisionNameResponse>> findNamesByDivisionId(final UUID countryId,
            final UUID divisionId) {
        if (countryId == null || divisionId == null) {
            return Uni.createFrom().item(List.of());
        }
        return repository.findNamesByDivisionId(CountryId.of(countryId), DivisionId.of(divisionId))
                .onItem().transform(names -> names.stream()
                        .map(AdministrativeDivisionApplicationMapper::toNameResponse)
                        .toList());
    }
}

package com.alexastudillo.geographicreference.application.service;

import com.alexastudillo.geographicreference.application.dto.CountryNameResponse;
import com.alexastudillo.geographicreference.application.dto.CountryResponse;
import com.alexastudillo.geographicreference.application.mapper.CountryApplicationMapper;
import com.alexastudillo.geographicreference.application.port.input.GetCountryQueryPort;
import com.alexastudillo.geographicreference.domain.exception.DomainException;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.Alpha2Code;
import com.alexastudillo.geographicreference.domain.model.valobj.Alpha3Code;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.NumericCode;
import com.alexastudillo.geographicreference.domain.port.output.CountryRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service implementing country read queries.
 */
public class GetCountryQueryService implements GetCountryQueryPort {

    private final CountryRepository countryRepository;

    public GetCountryQueryService(final CountryRepository countryRepository) {
        this.countryRepository = Objects.requireNonNull(countryRepository, "CountryRepository must not be null");
    }

    @Override
    public Optional<CountryResponse> findById(final UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return countryRepository.findById(CountryId.of(id))
                .map(CountryApplicationMapper::toResponse);
    }

    @Override
    public Optional<CountryResponse> findByAlpha2Code(final String alpha2Code) {
        if (alpha2Code == null || alpha2Code.isBlank()) {
            return Optional.empty();
        }
        try {
            final Alpha2Code code = Alpha2Code.of(alpha2Code.trim().toUpperCase());
            return countryRepository.findByAlpha2Code(code)
                    .map(CountryApplicationMapper::toResponse);
        } catch (DomainException _) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<CountryResponse> findByAlpha3Code(final String alpha3Code) {
        if (alpha3Code == null || alpha3Code.isBlank()) {
            return Optional.empty();
        }
        try {
            final Alpha3Code code = Alpha3Code.of(alpha3Code.trim().toUpperCase());
            return countryRepository.findByAlpha3Code(code)
                    .map(CountryApplicationMapper::toResponse);
        } catch (DomainException _) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<CountryResponse> findByNumericCode(final String numericCode) {
        if (numericCode == null || numericCode.isBlank()) {
            return Optional.empty();
        }
        try {
            final NumericCode code = NumericCode.of(numericCode.trim());
            return countryRepository.findByNumericCode(code)
                    .map(CountryApplicationMapper::toResponse);
        } catch (DomainException _) {
            return Optional.empty();
        }
    }

    @Override
    public List<CountryResponse> listAll() {
        return countryRepository.findAll().stream()
                .map(CountryApplicationMapper::toResponse)
                .toList();
    }

    @Override
    public List<CountryResponse> listByStatus(final String status) {
        if (status == null || status.isBlank()) {
            return List.of();
        }
        try {
            final GeographicRecordStatus recordStatus = GeographicRecordStatus.valueOf(status.trim().toUpperCase());
            return countryRepository.findByStatus(recordStatus).stream()
                    .map(CountryApplicationMapper::toResponse)
                    .toList();
        } catch (IllegalArgumentException _) {
            return List.of();
        }
    }

    @Override
    public List<CountryNameResponse> findNamesByCountryId(final UUID countryId) {
        if (countryId == null) {
            return List.of();
        }
        return countryRepository.findNamesByCountryId(CountryId.of(countryId)).stream()
                .map(CountryApplicationMapper::toNameResponse)
                .toList();
    }
}

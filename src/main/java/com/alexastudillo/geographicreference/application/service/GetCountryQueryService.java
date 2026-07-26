package com.alexastudillo.geographicreference.application.service;

import com.alexastudillo.geographicreference.application.dto.CountryNameResponse;
import com.alexastudillo.geographicreference.application.dto.CountryResponse;
import com.alexastudillo.geographicreference.application.mapper.CountryApplicationMapper;
import com.alexastudillo.geographicreference.application.port.input.GetCountryQueryPort;
import com.alexastudillo.geographicreference.application.port.output.CountryRepository;
import com.alexastudillo.geographicreference.domain.exception.DomainException;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.Alpha2Code;
import com.alexastudillo.geographicreference.domain.model.valobj.Alpha3Code;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.NumericCode;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.util.Objects;
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
    public Uni<CountryResponse> findById(final UUID id) {
        if (id == null) {
            return Uni.createFrom().nullItem();
        }
        return countryRepository.findById(CountryId.of(id))
                .onItem().ifNotNull().transform(CountryApplicationMapper::toResponse);
    }

    @Override
    public Uni<CountryResponse> findByAlpha2Code(final String alpha2Code) {
        if (alpha2Code == null || alpha2Code.isBlank()) {
            return Uni.createFrom().nullItem();
        }
        try {
            final Alpha2Code code = Alpha2Code.of(alpha2Code.trim().toUpperCase());
            return countryRepository.findByAlpha2Code(code)
                    .onItem().ifNotNull().transform(CountryApplicationMapper::toResponse);
        } catch (DomainException _) {
            return Uni.createFrom().nullItem();
        }
    }

    @Override
    public Uni<CountryResponse> findByAlpha3Code(final String alpha3Code) {
        if (alpha3Code == null || alpha3Code.isBlank()) {
            return Uni.createFrom().nullItem();
        }
        try {
            final Alpha3Code code = Alpha3Code.of(alpha3Code.trim().toUpperCase());
            return countryRepository.findByAlpha3Code(code)
                    .onItem().ifNotNull().transform(CountryApplicationMapper::toResponse);
        } catch (DomainException _) {
            return Uni.createFrom().nullItem();
        }
    }

    @Override
    public Uni<CountryResponse> findByNumericCode(final String numericCode) {
        if (numericCode == null || numericCode.isBlank()) {
            return Uni.createFrom().nullItem();
        }
        try {
            final NumericCode code = NumericCode.of(numericCode.trim());
            return countryRepository.findByNumericCode(code)
                    .onItem().ifNotNull().transform(CountryApplicationMapper::toResponse);
        } catch (DomainException _) {
            return Uni.createFrom().nullItem();
        }
    }

    @Override
    public Multi<CountryResponse> listAll() {
        return countryRepository.findAll()
                .onItem().transform(CountryApplicationMapper::toResponse);
    }

    @Override
    public Multi<CountryResponse> listByStatus(final String status) {
        if (status == null || status.isBlank()) {
            return Multi.createFrom().empty();
        }
        try {
            final GeographicRecordStatus recordStatus = GeographicRecordStatus.valueOf(status.trim().toUpperCase());
            return countryRepository.findByStatus(recordStatus)
                    .onItem().transform(CountryApplicationMapper::toResponse);
        } catch (IllegalArgumentException _) {
            return Multi.createFrom().empty();
        }
    }

    @Override
    public Multi<CountryNameResponse> findNamesByCountryId(final UUID countryId) {
        if (countryId == null) {
            return Multi.createFrom().empty();
        }
        return countryRepository.findNamesByCountryId(CountryId.of(countryId))
                .onItem().transform(CountryApplicationMapper::toNameResponse);
    }
}

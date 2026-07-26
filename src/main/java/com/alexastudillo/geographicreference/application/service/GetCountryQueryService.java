package com.alexastudillo.geographicreference.application.service;

import com.alexastudillo.geographicreference.application.dto.CountryNameLookupResponse;
import com.alexastudillo.geographicreference.application.dto.CountryNameResponse;
import com.alexastudillo.geographicreference.application.dto.CountryResponse;
import com.alexastudillo.geographicreference.application.mapper.CountryApplicationMapper;
import com.alexastudillo.geographicreference.application.port.input.GetCountryQueryPort;
import com.alexastudillo.geographicreference.application.port.output.CountryRepository;
import com.alexastudillo.geographicreference.domain.exception.DomainException;
import com.alexastudillo.geographicreference.domain.model.enums.CountryCodeType;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicNameType;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.Alpha2Code;
import com.alexastudillo.geographicreference.domain.model.valobj.Alpha3Code;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.LanguageTag;
import com.alexastudillo.geographicreference.domain.model.valobj.NumericCode;
import io.smallrye.mutiny.Uni;

import java.util.List;
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
    public Uni<List<CountryResponse>> listAll() {
        return countryRepository.findAll()
                .onItem().transform(countries -> countries.stream()
                        .map(CountryApplicationMapper::toResponse)
                        .toList());
    }

    @Override
    public Uni<List<CountryResponse>> listByStatus(final String status) {
        if (status == null || status.isBlank()) {
            return Uni.createFrom().item(List.of());
        }
        try {
            final GeographicRecordStatus recordStatus = GeographicRecordStatus.valueOf(status.trim().toUpperCase());
            return countryRepository.findByStatus(recordStatus)
                    .onItem().transform(countries -> countries.stream()
                            .map(CountryApplicationMapper::toResponse)
                            .toList());
        } catch (IllegalArgumentException _) {
            return Uni.createFrom().item(List.of());
        }
    }

    @Override
    public Uni<List<CountryNameResponse>> findNamesByCountryId(final UUID countryId) {
        if (countryId == null) {
            return Uni.createFrom().item(List.of());
        }
        return countryRepository.findNamesByCountryId(CountryId.of(countryId))
                .onItem().transform(names -> names.stream()
                        .map(CountryApplicationMapper::toNameResponse)
                        .toList());
    }

    @Override
    public Uni<List<CountryNameLookupResponse>> findNames(
            final String codeType,
            final String nameType,
            final String languageTag
    ) {
        if (isBlank(codeType) || isBlank(nameType) || isBlank(languageTag)) {
            return Uni.createFrom().item(List.of());
        }
        try {
            final CountryCodeType selectedCodeType =
                    CountryCodeType.valueOf(codeType.trim().toUpperCase());
            final GeographicNameType selectedNameType =
                    GeographicNameType.valueOf(nameType.trim().toUpperCase());
            final LanguageTag selectedLanguageTag = LanguageTag.of(languageTag.trim());

            return countryRepository.findNames(selectedCodeType, selectedNameType, selectedLanguageTag)
                    .onItem().transform(names -> names.stream()
                            .map(CountryApplicationMapper::toNameLookupResponse)
                            .toList());
        } catch (IllegalArgumentException | DomainException _) {
            return Uni.createFrom().item(List.of());
        }
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}

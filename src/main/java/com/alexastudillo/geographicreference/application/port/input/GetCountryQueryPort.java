package com.alexastudillo.geographicreference.application.port.input;

import com.alexastudillo.geographicreference.application.dto.CountryNameResponse;
import com.alexastudillo.geographicreference.application.dto.CountryNameLookupResponse;
import com.alexastudillo.geographicreference.application.dto.CountryResponse;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.UUID;

/**
 * Input port for country query use cases (read-only).
 */
public interface GetCountryQueryPort {

    Uni<CountryResponse> findById(UUID id);

    Uni<CountryResponse> findByAlpha2Code(String alpha2Code);

    Uni<CountryResponse> findByAlpha3Code(String alpha3Code);

    Uni<CountryResponse> findByNumericCode(String numericCode);

    Uni<List<CountryResponse>> listAll();

    Uni<List<CountryResponse>> listByStatus(String status);

    Uni<List<CountryNameResponse>> findNamesByCountryId(UUID countryId);

    Uni<List<CountryNameLookupResponse>> findNames(
            String codeType,
            String nameType,
            String languageTag
    );
}

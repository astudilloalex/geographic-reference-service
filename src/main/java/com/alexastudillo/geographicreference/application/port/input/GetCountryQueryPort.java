package com.alexastudillo.geographicreference.application.port.input;

import com.alexastudillo.geographicreference.application.dto.CountryNameResponse;
import com.alexastudillo.geographicreference.application.dto.CountryResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Input port for country query use cases (read-only).
 */
public interface GetCountryQueryPort {

    Optional<CountryResponse> findById(UUID id);

    Optional<CountryResponse> findByAlpha2Code(String alpha2Code);

    Optional<CountryResponse> findByAlpha3Code(String alpha3Code);

    Optional<CountryResponse> findByNumericCode(String numericCode);

    List<CountryResponse> listAll();

    List<CountryResponse> listByStatus(String status);

    List<CountryNameResponse> findNamesByCountryId(UUID countryId);
}

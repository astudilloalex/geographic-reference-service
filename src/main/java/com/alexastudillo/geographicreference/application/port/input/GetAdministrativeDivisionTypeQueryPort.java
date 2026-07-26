package com.alexastudillo.geographicreference.application.port.input;

import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionTypeResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Input port for administrative division type query use cases (read-only).
 */
public interface GetAdministrativeDivisionTypeQueryPort {

    Optional<AdministrativeDivisionTypeResponse> findById(UUID id);

    Optional<AdministrativeDivisionTypeResponse> findByCountryIdAndCode(UUID countryId, String code);

    List<AdministrativeDivisionTypeResponse> listByCountryId(UUID countryId);

    List<AdministrativeDivisionTypeResponse> listByCountryIdAndStatus(UUID countryId, String status);
}

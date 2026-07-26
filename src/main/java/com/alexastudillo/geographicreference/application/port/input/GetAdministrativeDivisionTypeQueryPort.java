package com.alexastudillo.geographicreference.application.port.input;

import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionTypeResponse;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.UUID;

/**
 * Input port for administrative division type query use cases (read-only).
 */
public interface GetAdministrativeDivisionTypeQueryPort {

    Uni<AdministrativeDivisionTypeResponse> findById(UUID id);

    Uni<AdministrativeDivisionTypeResponse> findByCountryIdAndCode(UUID countryId, String code);

    Uni<List<AdministrativeDivisionTypeResponse>> listByCountryId(UUID countryId);

    Uni<List<AdministrativeDivisionTypeResponse>> listByCountryIdAndStatus(UUID countryId, String status);
}

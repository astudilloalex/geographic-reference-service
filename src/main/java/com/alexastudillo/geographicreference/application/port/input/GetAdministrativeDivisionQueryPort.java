package com.alexastudillo.geographicreference.application.port.input;

import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionIdentifierResponse;
import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionNameResponse;
import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Input port for administrative division query use cases (read-only).
 */
public interface GetAdministrativeDivisionQueryPort {

    Optional<AdministrativeDivisionResponse> findById(UUID id);

    Optional<AdministrativeDivisionResponse> findByCanonicalCode(UUID countryId, String canonicalCode);

    List<AdministrativeDivisionResponse> listByCountryId(UUID countryId);

    List<AdministrativeDivisionResponse> listByParentId(UUID countryId, UUID parentId);

    List<AdministrativeDivisionResponse> listByTypeAndStatus(UUID countryId, UUID typeId, String status);

    List<AdministrativeDivisionIdentifierResponse> findIdentifiersByDivisionId(UUID countryId, UUID divisionId);

    List<AdministrativeDivisionNameResponse> findNamesByDivisionId(UUID countryId, UUID divisionId);
}

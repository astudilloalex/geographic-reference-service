package com.alexastudillo.geographicreference.application.port.input;

import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionIdentifierResponse;
import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionNameResponse;
import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionResponse;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.util.UUID;

/**
 * Input port for administrative division query use cases (read-only).
 */
public interface GetAdministrativeDivisionQueryPort {

    Uni<AdministrativeDivisionResponse> findById(UUID id);

    Uni<AdministrativeDivisionResponse> findByCanonicalCode(UUID countryId, String canonicalCode);

    Multi<AdministrativeDivisionResponse> listByCountryId(UUID countryId);

    Multi<AdministrativeDivisionResponse> listByParentId(UUID countryId, UUID parentId);

    Multi<AdministrativeDivisionResponse> listByTypeAndStatus(UUID countryId, UUID typeId, String status);

    Multi<AdministrativeDivisionIdentifierResponse> findIdentifiersByDivisionId(UUID countryId, UUID divisionId);

    Multi<AdministrativeDivisionNameResponse> findNamesByDivisionId(UUID countryId, UUID divisionId);
}

package com.alexastudillo.geographicreference.application.port.input;

import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionIdentifierResponse;
import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionNameResponse;
import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionResponse;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.UUID;

/**
 * Input port for administrative division query use cases (read-only).
 */
public interface GetAdministrativeDivisionQueryPort {

    Uni<AdministrativeDivisionResponse> findById(UUID id);

    Uni<AdministrativeDivisionResponse> findByCanonicalCode(UUID countryId, String canonicalCode);

    Uni<List<AdministrativeDivisionResponse>> listByCountryId(UUID countryId);

    Uni<List<AdministrativeDivisionResponse>> listByParentId(UUID countryId, UUID parentId);

    Uni<List<AdministrativeDivisionResponse>> listByTypeAndStatus(UUID countryId, UUID typeId, String status);

    Uni<List<AdministrativeDivisionIdentifierResponse>> findIdentifiersByDivisionId(UUID countryId, UUID divisionId);

    Uni<List<AdministrativeDivisionNameResponse>> findNamesByDivisionId(UUID countryId, UUID divisionId);
}

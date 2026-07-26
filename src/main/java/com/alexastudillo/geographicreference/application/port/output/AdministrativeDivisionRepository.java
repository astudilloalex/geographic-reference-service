package com.alexastudillo.geographicreference.application.port.output;

import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivision;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivisionIdentifier;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivisionName;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionTypeId;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * Output port for the {@link AdministrativeDivision} aggregate persistence.
 *
 * <p>
 * Covers divisions, their external identifiers, and localized names.
 */
public interface AdministrativeDivisionRepository {

    // ── Divisions ──────────────────────────────────────────────────────────

    Uni<AdministrativeDivision> findById(DivisionId id);

    Uni<AdministrativeDivision> findByCanonicalCode(CountryId countryId, String canonicalCode);

    Multi<AdministrativeDivision> findByCountryId(CountryId countryId);

    Multi<AdministrativeDivision> findByParentDivisionId(CountryId countryId, DivisionId parentId);

    Multi<AdministrativeDivision> findByTypeAndStatus(CountryId countryId,
            DivisionTypeId typeId,
            GeographicRecordStatus status);

    Uni<AdministrativeDivision> save(AdministrativeDivision division);

    // ── Identifiers ────────────────────────────────────────────────────────

    Multi<AdministrativeDivisionIdentifier> findIdentifiersByDivisionId(CountryId countryId,
            DivisionId divisionId);

    Uni<AdministrativeDivisionIdentifier> saveIdentifier(AdministrativeDivisionIdentifier identifier);

    // ── Names ──────────────────────────────────────────────────────────────

    Multi<AdministrativeDivisionName> findNamesByDivisionId(CountryId countryId,
            DivisionId divisionId);

    Uni<AdministrativeDivisionName> saveName(AdministrativeDivisionName name);
}

package com.alexastudillo.geographicreference.application.port.output;

import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivision;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivisionIdentifier;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivisionName;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionTypeId;
import io.smallrye.mutiny.Uni;

import java.util.List;

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

    Uni<List<AdministrativeDivision>> findByCountryId(CountryId countryId);

    Uni<List<AdministrativeDivision>> findByParentDivisionId(CountryId countryId, DivisionId parentId);

    Uni<List<AdministrativeDivision>> findByTypeAndStatus(CountryId countryId,
            DivisionTypeId typeId,
            GeographicRecordStatus status);

    // ── Identifiers ────────────────────────────────────────────────────────

    Uni<List<AdministrativeDivisionIdentifier>> findIdentifiersByDivisionId(CountryId countryId,
            DivisionId divisionId);

    // ── Names ──────────────────────────────────────────────────────────────

    Uni<List<AdministrativeDivisionName>> findNamesByDivisionId(CountryId countryId,
            DivisionId divisionId);
}

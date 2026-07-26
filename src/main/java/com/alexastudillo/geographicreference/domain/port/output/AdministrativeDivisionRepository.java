package com.alexastudillo.geographicreference.domain.port.output;

import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivision;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivisionIdentifier;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivisionName;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionTypeId;

import java.util.List;
import java.util.Optional;

/**
 * Output port for the {@link AdministrativeDivision} aggregate persistence.
 *
 * <p>Covers divisions, their external identifiers, and localized names.
 */
public interface AdministrativeDivisionRepository {

    // ── Divisions ──────────────────────────────────────────────────────────

    Optional<AdministrativeDivision> findById(DivisionId id);

    Optional<AdministrativeDivision> findByCanonicalCode(CountryId countryId, String canonicalCode);

    List<AdministrativeDivision> findByCountryId(CountryId countryId);

    List<AdministrativeDivision> findByParentDivisionId(CountryId countryId, DivisionId parentId);

    List<AdministrativeDivision> findByTypeAndStatus(CountryId countryId,
                                                     DivisionTypeId typeId,
                                                     GeographicRecordStatus status);

    AdministrativeDivision save(AdministrativeDivision division);

    // ── Identifiers ────────────────────────────────────────────────────────

    List<AdministrativeDivisionIdentifier> findIdentifiersByDivisionId(CountryId countryId,
                                                                       DivisionId divisionId);

    AdministrativeDivisionIdentifier saveIdentifier(AdministrativeDivisionIdentifier identifier);

    // ── Names ──────────────────────────────────────────────────────────────

    List<AdministrativeDivisionName> findNamesByDivisionId(CountryId countryId,
                                                           DivisionId divisionId);

    AdministrativeDivisionName saveName(AdministrativeDivisionName name);
}

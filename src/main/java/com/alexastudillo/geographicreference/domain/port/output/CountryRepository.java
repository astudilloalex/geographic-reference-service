package com.alexastudillo.geographicreference.domain.port.output;

import com.alexastudillo.geographicreference.domain.model.entity.Country;
import com.alexastudillo.geographicreference.domain.model.entity.CountryName;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.Alpha2Code;
import com.alexastudillo.geographicreference.domain.model.valobj.Alpha3Code;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.NumericCode;

import java.util.List;
import java.util.Optional;

/**
 * Output port for the {@link Country} aggregate persistence.
 *
 * <p>Implementations reside in the infrastructure layer and must not leak
 * framework-specific types into the domain.
 */
public interface CountryRepository {

    Optional<Country> findById(CountryId id);

    Optional<Country> findByAlpha2Code(Alpha2Code code);

    Optional<Country> findByAlpha3Code(Alpha3Code code);

    Optional<Country> findByNumericCode(NumericCode code);

    List<Country> findByStatus(GeographicRecordStatus status);

    List<Country> findAll();

    Country save(Country country);

    // ── Country Names ──────────────────────────────────────────────────────

    List<CountryName> findNamesByCountryId(CountryId countryId);

    CountryName saveName(CountryName name);
}

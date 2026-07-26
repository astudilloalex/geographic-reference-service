package com.alexastudillo.geographicreference.application.port.output;

import com.alexastudillo.geographicreference.domain.model.entity.Country;
import com.alexastudillo.geographicreference.domain.model.entity.CountryName;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.Alpha2Code;
import com.alexastudillo.geographicreference.domain.model.valobj.Alpha3Code;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.NumericCode;
import io.smallrye.mutiny.Uni;

import java.util.List;

/**
 * Output port for the {@link Country} aggregate persistence.
 *
 * <p>Implementations reside in the infrastructure layer and must not leak
 * framework-specific types into the domain.
 */
public interface CountryRepository {

    Uni<Country> findById(CountryId id);

    Uni<Country> findByAlpha2Code(Alpha2Code code);

    Uni<Country> findByAlpha3Code(Alpha3Code code);

    Uni<Country> findByNumericCode(NumericCode code);

    Uni<List<Country>> findByStatus(GeographicRecordStatus status);

    Uni<List<Country>> findAll();

    Uni<Country> save(Country country);

    // ── Country Names ──────────────────────────────────────────────────────

    Uni<List<CountryName>> findNamesByCountryId(CountryId countryId);

    Uni<CountryName> saveName(CountryName name);
}

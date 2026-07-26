package com.alexastudillo.geographicreference.application.port.output;

import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivisionType;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionTypeId;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * Output port for {@link AdministrativeDivisionType} persistence.
 *
 * <p>Division types define the hierarchical structure of administrative
 * divisions within a country (e.g. Province → Canton → Parish).
 */
public interface AdministrativeDivisionTypeRepository {

    Uni<AdministrativeDivisionType> findById(DivisionTypeId id);

    Uni<AdministrativeDivisionType> findByCountryIdAndCode(CountryId countryId, String code);

    Multi<AdministrativeDivisionType> findByCountryId(CountryId countryId);

    Multi<AdministrativeDivisionType> findByCountryIdAndStatus(CountryId countryId, GeographicRecordStatus status);

    Uni<AdministrativeDivisionType> save(AdministrativeDivisionType type);
}

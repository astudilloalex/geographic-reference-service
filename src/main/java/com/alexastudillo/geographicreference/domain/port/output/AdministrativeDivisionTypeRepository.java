package com.alexastudillo.geographicreference.domain.port.output;

import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivisionType;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionTypeId;

import java.util.List;
import java.util.Optional;

/**
 * Output port for {@link AdministrativeDivisionType} persistence.
 *
 * <p>Division types define the hierarchical structure of administrative
 * divisions within a country (e.g. Province → Canton → Parish).
 */
public interface AdministrativeDivisionTypeRepository {

    Optional<AdministrativeDivisionType> findById(DivisionTypeId id);

    Optional<AdministrativeDivisionType> findByCountryIdAndCode(CountryId countryId, String code);

    List<AdministrativeDivisionType> findByCountryId(CountryId countryId);

    List<AdministrativeDivisionType> findByCountryIdAndStatus(CountryId countryId, GeographicRecordStatus status);

    AdministrativeDivisionType save(AdministrativeDivisionType type);
}

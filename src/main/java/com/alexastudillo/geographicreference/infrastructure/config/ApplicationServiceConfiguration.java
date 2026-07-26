package com.alexastudillo.geographicreference.infrastructure.config;

import com.alexastudillo.geographicreference.application.port.input.GetAdministrativeDivisionQueryPort;
import com.alexastudillo.geographicreference.application.port.input.GetAdministrativeDivisionTypeQueryPort;
import com.alexastudillo.geographicreference.application.port.input.GetCountryQueryPort;
import com.alexastudillo.geographicreference.application.port.output.AdministrativeDivisionRepository;
import com.alexastudillo.geographicreference.application.port.output.AdministrativeDivisionTypeRepository;
import com.alexastudillo.geographicreference.application.port.output.CountryRepository;
import com.alexastudillo.geographicreference.application.service.GetAdministrativeDivisionQueryService;
import com.alexastudillo.geographicreference.application.service.GetAdministrativeDivisionTypeQueryService;
import com.alexastudillo.geographicreference.application.service.GetCountryQueryService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * Infrastructure composition root for framework-independent application services.
 */
@ApplicationScoped
public class ApplicationServiceConfiguration {

    @Produces
    @ApplicationScoped
    public GetCountryQueryPort countryQueryPort(final CountryRepository repository) {
        return new GetCountryQueryService(repository);
    }

    @Produces
    @ApplicationScoped
    public GetAdministrativeDivisionTypeQueryPort administrativeDivisionTypeQueryPort(
            final AdministrativeDivisionTypeRepository repository
    ) {
        return new GetAdministrativeDivisionTypeQueryService(repository);
    }

    @Produces
    @ApplicationScoped
    public GetAdministrativeDivisionQueryPort administrativeDivisionQueryPort(
            final AdministrativeDivisionRepository repository
    ) {
        return new GetAdministrativeDivisionQueryService(repository);
    }
}

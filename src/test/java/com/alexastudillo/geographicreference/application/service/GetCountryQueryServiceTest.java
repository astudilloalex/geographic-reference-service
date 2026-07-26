package com.alexastudillo.geographicreference.application.service;

import com.alexastudillo.geographicreference.application.dto.CountryNameResponse;
import com.alexastudillo.geographicreference.application.dto.CountryResponse;
import com.alexastudillo.geographicreference.domain.model.entity.Country;
import com.alexastudillo.geographicreference.domain.model.entity.CountryName;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicNameType;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.Alpha2Code;
import com.alexastudillo.geographicreference.domain.model.valobj.Alpha3Code;
import com.alexastudillo.geographicreference.domain.model.valobj.AuditInfo;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.LanguageTag;
import com.alexastudillo.geographicreference.domain.model.valobj.NumericCode;
import com.alexastudillo.geographicreference.domain.model.valobj.SourceProvenance;
import com.alexastudillo.geographicreference.domain.model.valobj.ValidityPeriod;
import com.alexastudillo.geographicreference.domain.port.output.CountryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCountryQueryServiceTest {

    @Mock
    private CountryRepository countryRepository;

    private GetCountryQueryService service;

    private final CountryId countryId = CountryId.of(UUID.randomUUID());
    private final AuditInfo auditInfo = AuditInfo.create("test-user");
    private final ValidityPeriod validity = ValidityPeriod.unbounded();
    private final SourceProvenance provenance = SourceProvenance.of("ISO");
    private final Country country = new Country(
            countryId,
            Alpha2Code.of("EC"),
            Alpha3Code.of("ECU"),
            NumericCode.of("218"),
            "Ecuador",
            "Republic of Ecuador",
            true,
            GeographicRecordStatus.ACTIVE,
            validity,
            provenance,
            auditInfo);

    @BeforeEach
    void setUp() {
        service = new GetCountryQueryService(countryRepository);
    }

    @Test
    void shouldRejectNullRepositoryInConstructor() {
        assertThatThrownBy(() -> new GetCountryQueryService(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldFindById() {
        assertThat(service.findById(null)).isEmpty();

        when(countryRepository.findById(countryId)).thenReturn(Optional.of(country));
        final Optional<CountryResponse> result = service.findById(countryId.value());

        assertThat(result).isPresent();
        assertThat(result.get().alpha2Code()).isEqualTo("EC");
    }

    @Test
    void shouldFindByAlpha2Code() {
        assertThat(service.findByAlpha2Code(null)).isEmpty();
        assertThat(service.findByAlpha2Code(" ")).isEmpty();
        assertThat(service.findByAlpha2Code("INVALID")).isEmpty();

        when(countryRepository.findByAlpha2Code(Alpha2Code.of("EC"))).thenReturn(Optional.of(country));
        final Optional<CountryResponse> result = service.findByAlpha2Code("ec");

        assertThat(result).isPresent();
        assertThat(result.get().alpha2Code()).isEqualTo("EC");
    }

    @Test
    void shouldFindByAlpha3Code() {
        assertThat(service.findByAlpha3Code(null)).isEmpty();
        assertThat(service.findByAlpha3Code(" ")).isEmpty();
        assertThat(service.findByAlpha3Code("INVALID")).isEmpty();

        when(countryRepository.findByAlpha3Code(Alpha3Code.of("ECU"))).thenReturn(Optional.of(country));
        final Optional<CountryResponse> result = service.findByAlpha3Code("ecu");

        assertThat(result).isPresent();
        assertThat(result.get().alpha3Code()).isEqualTo("ECU");
    }

    @Test
    void shouldFindByNumericCode() {
        assertThat(service.findByNumericCode(null)).isEmpty();
        assertThat(service.findByNumericCode(" ")).isEmpty();
        assertThat(service.findByNumericCode("INVALID")).isEmpty();

        when(countryRepository.findByNumericCode(NumericCode.of("218"))).thenReturn(Optional.of(country));
        final Optional<CountryResponse> result = service.findByNumericCode("218");

        assertThat(result).isPresent();
        assertThat(result.get().numericCode()).isEqualTo("218");
    }

    @Test
    void shouldListAll() {
        when(countryRepository.findAll()).thenReturn(List.of(country));
        final List<CountryResponse> list = service.listAll();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).alpha2Code()).isEqualTo("EC");
    }

    @Test
    void shouldListByStatus() {
        assertThat(service.listByStatus(null)).isEmpty();
        assertThat(service.listByStatus(" ")).isEmpty();
        assertThat(service.listByStatus("INVALID_STATUS")).isEmpty();

        when(countryRepository.findByStatus(GeographicRecordStatus.ACTIVE)).thenReturn(List.of(country));
        final List<CountryResponse> list = service.listByStatus("active");

        assertThat(list).hasSize(1);
        assertThat(list.get(0).status()).isEqualTo("ACTIVE");
    }

    @Test
    void shouldFindNamesByCountryId() {
        assertThat(service.findNamesByCountryId(null)).isEmpty();

        final UUID nameId = UUID.randomUUID();
        final CountryName countryName = new CountryName(
                nameId,
                countryId,
                LanguageTag.of("es"),
                GeographicNameType.OFFICIAL,
                "República del Ecuador",
                true,
                validity,
                auditInfo);

        when(countryRepository.findNamesByCountryId(countryId)).thenReturn(List.of(countryName));
        final List<CountryNameResponse> names = service.findNamesByCountryId(countryId.value());

        assertThat(names).hasSize(1);
        assertThat(names.get(0).name()).isEqualTo("República del Ecuador");
    }
}

package com.alexastudillo.geographicreference.application.service;

import com.alexastudillo.geographicreference.application.dto.CountryNameLookupResponse;
import com.alexastudillo.geographicreference.application.dto.CountryNameResponse;
import com.alexastudillo.geographicreference.application.dto.CountryResponse;
import com.alexastudillo.geographicreference.application.port.output.CountryRepository;
import com.alexastudillo.geographicreference.domain.model.entity.Country;
import com.alexastudillo.geographicreference.domain.model.entity.CountryName;
import com.alexastudillo.geographicreference.domain.model.enums.CountryCodeType;
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
import com.alexastudillo.geographicreference.domain.model.projection.CountryNameLookup;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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
        assertThat(service.findById(null).await().indefinitely()).isNull();

        when(countryRepository.findById(countryId)).thenReturn(Uni.createFrom().item(country));
        final CountryResponse result = service.findById(countryId.value()).await().indefinitely();

        assertThat(result).isNotNull();
        assertThat(result.alpha2Code()).isEqualTo("EC");
    }

    @Test
    void shouldFindByAlpha2Code() {
        assertThat(service.findByAlpha2Code(null).await().indefinitely()).isNull();
        assertThat(service.findByAlpha2Code(" ").await().indefinitely()).isNull();
        assertThat(service.findByAlpha2Code("INVALID").await().indefinitely()).isNull();

        when(countryRepository.findByAlpha2Code(Alpha2Code.of("EC"))).thenReturn(Uni.createFrom().item(country));
        final CountryResponse result = service.findByAlpha2Code("ec").await().indefinitely();

        assertThat(result).isNotNull();
        assertThat(result.alpha2Code()).isEqualTo("EC");
    }

    @Test
    void shouldFindByAlpha3Code() {
        assertThat(service.findByAlpha3Code(null).await().indefinitely()).isNull();
        assertThat(service.findByAlpha3Code(" ").await().indefinitely()).isNull();
        assertThat(service.findByAlpha3Code("INVALID").await().indefinitely()).isNull();

        when(countryRepository.findByAlpha3Code(Alpha3Code.of("ECU"))).thenReturn(Uni.createFrom().item(country));
        final CountryResponse result = service.findByAlpha3Code("ecu").await().indefinitely();

        assertThat(result).isNotNull();
        assertThat(result.alpha3Code()).isEqualTo("ECU");
    }

    @Test
    void shouldFindByNumericCode() {
        assertThat(service.findByNumericCode(null).await().indefinitely()).isNull();
        assertThat(service.findByNumericCode(" ").await().indefinitely()).isNull();
        assertThat(service.findByNumericCode("INVALID").await().indefinitely()).isNull();

        when(countryRepository.findByNumericCode(NumericCode.of("218"))).thenReturn(Uni.createFrom().item(country));
        final CountryResponse result = service.findByNumericCode("218").await().indefinitely();

        assertThat(result).isNotNull();
        assertThat(result.numericCode()).isEqualTo("218");
    }

    @Test
    void shouldListAll() {
        when(countryRepository.findAll()).thenReturn(Uni.createFrom().item(List.of(country)));
        final List<CountryResponse> list = service.listAll().await().indefinitely();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).alpha2Code()).isEqualTo("EC");
    }

    @Test
    void shouldListByStatus() {
        assertThat(service.listByStatus(null).await().indefinitely()).isEmpty();
        assertThat(service.listByStatus(" ").await().indefinitely()).isEmpty();
        assertThat(service.listByStatus("INVALID_STATUS").await().indefinitely()).isEmpty();

        when(countryRepository.findByStatus(GeographicRecordStatus.ACTIVE))
                .thenReturn(Uni.createFrom().item(List.of(country)));
        final List<CountryResponse> list = service.listByStatus("active").await().indefinitely();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).status()).isEqualTo("ACTIVE");
    }

    @Test
    void shouldFindNamesByCountryId() {
        assertThat(service.findNamesByCountryId(null).await().indefinitely()).isEmpty();

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

        when(countryRepository.findNamesByCountryId(countryId))
                .thenReturn(Uni.createFrom().item(List.of(countryName)));
        final List<CountryNameResponse> names = service.findNamesByCountryId(countryId.value())
                .await().indefinitely();

        assertThat(names).hasSize(1);
        assertThat(names.get(0).name()).isEqualTo("República del Ecuador");
    }

    @Test
    void shouldFindNamesByCodeTypeNameTypeAndLanguageTag() {
        final CountryNameLookup lookup = new CountryNameLookup(
                CountryCodeType.ALPHA2,
                "EC",
                LanguageTag.of("es"),
                GeographicNameType.COMMON,
                "Ecuador",
                true);
        when(countryRepository.findNames(
                CountryCodeType.ALPHA2,
                GeographicNameType.COMMON,
                LanguageTag.of("ES")))
                .thenReturn(Uni.createFrom().item(List.of(lookup)));

        final List<CountryNameLookupResponse> names =
                service.findNames(" alpha2 ", " common ", "ES").await().indefinitely();

        assertThat(names).singleElement().satisfies(name -> {
            assertThat(name.codeType()).isEqualTo("ALPHA2");
            assertThat(name.code()).isEqualTo("EC");
            assertThat(name.languageTag()).isEqualTo("es");
            assertThat(name.nameType()).isEqualTo("COMMON");
            assertThat(name.name()).isEqualTo("Ecuador");
            assertThat(name.preferred()).isTrue();
        });
    }

    @Test
    void shouldReturnEmptyCountryNameLookupForInvalidFilters() {
        assertThat(service.findNames(null, "COMMON", "es").await().indefinitely()).isEmpty();
        assertThat(service.findNames("ALPHA2", null, "es").await().indefinitely()).isEmpty();
        assertThat(service.findNames("ALPHA2", "COMMON", null).await().indefinitely()).isEmpty();
        assertThat(service.findNames("UUID", "COMMON", "es").await().indefinitely()).isEmpty();
        assertThat(service.findNames("ALPHA2", "CANONICAL", "es").await().indefinitely()).isEmpty();
        assertThat(service.findNames("ALPHA2", "COMMON", "not_a_tag").await().indefinitely()).isEmpty();
    }
}

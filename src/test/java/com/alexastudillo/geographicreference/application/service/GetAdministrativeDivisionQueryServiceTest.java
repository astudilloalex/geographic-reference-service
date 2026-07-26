package com.alexastudillo.geographicreference.application.service;

import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionIdentifierResponse;
import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionNameResponse;
import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionResponse;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivision;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivisionIdentifier;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivisionName;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicIdentifierStatus;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicNameType;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.AuditInfo;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionTypeId;
import com.alexastudillo.geographicreference.domain.model.valobj.LanguageTag;
import com.alexastudillo.geographicreference.domain.model.valobj.SourceProvenance;
import com.alexastudillo.geographicreference.domain.model.valobj.ValidityPeriod;
import com.alexastudillo.geographicreference.domain.port.output.AdministrativeDivisionRepository;
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
class GetAdministrativeDivisionQueryServiceTest {

    @Mock
    private AdministrativeDivisionRepository repository;

    private GetAdministrativeDivisionQueryService service;

    private final CountryId countryId = CountryId.of(UUID.randomUUID());
    private final DivisionTypeId typeId = DivisionTypeId.of(UUID.randomUUID());
    private final DivisionId divisionId = DivisionId.of(UUID.randomUUID());
    private final AuditInfo auditInfo = AuditInfo.create("test-user");
    private final ValidityPeriod validity = ValidityPeriod.unbounded();
    private final SourceProvenance provenance = SourceProvenance.of("ISO");

    private final AdministrativeDivision division = new AdministrativeDivision(
            divisionId,
            countryId,
            typeId,
            null,
            "P-PICHINCHA",
            "Pichincha",
            "Provincia de Pichincha",
            GeographicRecordStatus.ACTIVE,
            validity,
            provenance,
            auditInfo
    );

    @BeforeEach
    void setUp() {
        service = new GetAdministrativeDivisionQueryService(repository);
    }

    @Test
    void shouldRejectNullRepositoryInConstructor() {
        assertThatThrownBy(() -> new GetAdministrativeDivisionQueryService(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldFindById() {
        assertThat(service.findById(null)).isEmpty();

        when(repository.findById(divisionId)).thenReturn(Optional.of(division));
        final Optional<AdministrativeDivisionResponse> result = service.findById(divisionId.value());

        assertThat(result).isPresent();
        assertThat(result.get().canonicalCode()).isEqualTo("P-PICHINCHA");
    }

    @Test
    void shouldFindByCanonicalCode() {
        assertThat(service.findByCanonicalCode(null, "P-PICHINCHA")).isEmpty();
        assertThat(service.findByCanonicalCode(countryId.value(), null)).isEmpty();
        assertThat(service.findByCanonicalCode(countryId.value(), " ")).isEmpty();

        when(repository.findByCanonicalCode(countryId, "P-PICHINCHA")).thenReturn(Optional.of(division));
        final Optional<AdministrativeDivisionResponse> result = service.findByCanonicalCode(countryId.value(), "p-pichincha");

        assertThat(result).isPresent();
        assertThat(result.get().canonicalCode()).isEqualTo("P-PICHINCHA");
    }

    @Test
    void shouldListByCountryId() {
        assertThat(service.listByCountryId(null)).isEmpty();

        when(repository.findByCountryId(countryId)).thenReturn(List.of(division));
        final List<AdministrativeDivisionResponse> list = service.listByCountryId(countryId.value());

        assertThat(list).hasSize(1);
        assertThat(list.get(0).canonicalCode()).isEqualTo("P-PICHINCHA");
    }

    @Test
    void shouldListByParentId() {
        assertThat(service.listByParentId(null, UUID.randomUUID())).isEmpty();

        final UUID parentUuid = UUID.randomUUID();
        when(repository.findByParentDivisionId(countryId, DivisionId.of(parentUuid))).thenReturn(List.of(division));
        final List<AdministrativeDivisionResponse> list = service.listByParentId(countryId.value(), parentUuid);

        assertThat(list).hasSize(1);

        when(repository.findByParentDivisionId(countryId, null)).thenReturn(List.of(division));
        final List<AdministrativeDivisionResponse> rootList = service.listByParentId(countryId.value(), null);
        assertThat(rootList).hasSize(1);
    }

    @Test
    void shouldListByTypeAndStatus() {
        assertThat(service.listByTypeAndStatus(null, typeId.value(), "ACTIVE")).isEmpty();
        assertThat(service.listByTypeAndStatus(countryId.value(), null, "ACTIVE")).isEmpty();
        assertThat(service.listByTypeAndStatus(countryId.value(), typeId.value(), null)).isEmpty();
        assertThat(service.listByTypeAndStatus(countryId.value(), typeId.value(), " ")).isEmpty();
        assertThat(service.listByTypeAndStatus(countryId.value(), typeId.value(), "INVALID")).isEmpty();

        when(repository.findByTypeAndStatus(countryId, typeId, GeographicRecordStatus.ACTIVE)).thenReturn(List.of(division));
        final List<AdministrativeDivisionResponse> list = service.listByTypeAndStatus(countryId.value(), typeId.value(), "active");

        assertThat(list).hasSize(1);
        assertThat(list.get(0).status()).isEqualTo("ACTIVE");
    }

    @Test
    void shouldFindIdentifiersByDivisionId() {
        assertThat(service.findIdentifiersByDivisionId(null, divisionId.value())).isEmpty();
        assertThat(service.findIdentifiersByDivisionId(countryId.value(), null)).isEmpty();

        final UUID id = UUID.randomUUID();
        final AdministrativeDivisionIdentifier identifier = new AdministrativeDivisionIdentifier(
                id,
                countryId,
                divisionId,
                "ISO_3166_2",
                "EC-P",
                true,
                GeographicIdentifierStatus.ACTIVE,
                validity,
                provenance,
                auditInfo
        );

        when(repository.findIdentifiersByDivisionId(countryId, divisionId)).thenReturn(List.of(identifier));
        final List<AdministrativeDivisionIdentifierResponse> result = service.findIdentifiersByDivisionId(countryId.value(), divisionId.value());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).schemeCode()).isEqualTo("ISO_3166_2");
    }

    @Test
    void shouldFindNamesByDivisionId() {
        assertThat(service.findNamesByDivisionId(null, divisionId.value())).isEmpty();
        assertThat(service.findNamesByDivisionId(countryId.value(), null)).isEmpty();

        final UUID id = UUID.randomUUID();
        final AdministrativeDivisionName name = new AdministrativeDivisionName(
                id,
                countryId,
                divisionId,
                LanguageTag.of("es"),
                GeographicNameType.OFFICIAL,
                "Provincia de Pichincha",
                true,
                validity,
                auditInfo
        );

        when(repository.findNamesByDivisionId(countryId, divisionId)).thenReturn(List.of(name));
        final List<AdministrativeDivisionNameResponse> result = service.findNamesByDivisionId(countryId.value(), divisionId.value());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Provincia de Pichincha");
    }
}

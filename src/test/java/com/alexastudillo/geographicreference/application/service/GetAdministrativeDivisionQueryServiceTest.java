package com.alexastudillo.geographicreference.application.service;

import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionIdentifierResponse;
import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionNameResponse;
import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionResponse;
import com.alexastudillo.geographicreference.application.port.output.AdministrativeDivisionRepository;
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
        assertThat(service.findById(null).await().indefinitely()).isNull();

        when(repository.findById(divisionId)).thenReturn(Uni.createFrom().item(division));
        final AdministrativeDivisionResponse result = service.findById(divisionId.value()).await().indefinitely();

        assertThat(result).isNotNull();
        assertThat(result.canonicalCode()).isEqualTo("P-PICHINCHA");
    }

    @Test
    void shouldFindByCanonicalCode() {
        assertThat(service.findByCanonicalCode(null, "P-PICHINCHA").await().indefinitely()).isNull();
        assertThat(service.findByCanonicalCode(countryId.value(), null).await().indefinitely()).isNull();
        assertThat(service.findByCanonicalCode(countryId.value(), " ").await().indefinitely()).isNull();

        when(repository.findByCanonicalCode(countryId, "P-PICHINCHA")).thenReturn(Uni.createFrom().item(division));
        final AdministrativeDivisionResponse result = service.findByCanonicalCode(countryId.value(), "p-pichincha").await().indefinitely();

        assertThat(result).isNotNull();
        assertThat(result.canonicalCode()).isEqualTo("P-PICHINCHA");
    }

    @Test
    void shouldListByCountryId() {
        assertThat(service.listByCountryId(null).await().indefinitely()).isEmpty();

        when(repository.findByCountryId(countryId)).thenReturn(Uni.createFrom().item(List.of(division)));
        final List<AdministrativeDivisionResponse> list = service.listByCountryId(countryId.value())
                .await().indefinitely();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).canonicalCode()).isEqualTo("P-PICHINCHA");
    }

    @Test
    void shouldListByParentId() {
        assertThat(service.listByParentId(null, UUID.randomUUID()).await().indefinitely()).isEmpty();

        final UUID parentUuid = UUID.randomUUID();
        when(repository.findByParentDivisionId(countryId, DivisionId.of(parentUuid)))
                .thenReturn(Uni.createFrom().item(List.of(division)));
        final List<AdministrativeDivisionResponse> list = service.listByParentId(countryId.value(), parentUuid)
                .await().indefinitely();

        assertThat(list).hasSize(1);

        when(repository.findByParentDivisionId(countryId, null))
                .thenReturn(Uni.createFrom().item(List.of(division)));
        final List<AdministrativeDivisionResponse> rootList = service.listByParentId(countryId.value(), null)
                .await().indefinitely();
        assertThat(rootList).hasSize(1);
    }

    @Test
    void shouldListByTypeAndStatus() {
        assertThat(service.listByTypeAndStatus(null, typeId.value(), "ACTIVE").await().indefinitely()).isEmpty();
        assertThat(service.listByTypeAndStatus(countryId.value(), null, "ACTIVE").await().indefinitely()).isEmpty();
        assertThat(service.listByTypeAndStatus(countryId.value(), typeId.value(), null).await().indefinitely())
                .isEmpty();
        assertThat(service.listByTypeAndStatus(countryId.value(), typeId.value(), " ").await().indefinitely())
                .isEmpty();
        assertThat(service.listByTypeAndStatus(countryId.value(), typeId.value(), "INVALID").await().indefinitely())
                .isEmpty();

        when(repository.findByTypeAndStatus(countryId, typeId, GeographicRecordStatus.ACTIVE))
                .thenReturn(Uni.createFrom().item(List.of(division)));
        final List<AdministrativeDivisionResponse> list = service
                .listByTypeAndStatus(countryId.value(), typeId.value(), "active")
                .await().indefinitely();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).status()).isEqualTo("ACTIVE");
    }

    @Test
    void shouldFindIdentifiersByDivisionId() {
        assertThat(service.findIdentifiersByDivisionId(null, divisionId.value()).await().indefinitely()).isEmpty();
        assertThat(service.findIdentifiersByDivisionId(countryId.value(), null).await().indefinitely()).isEmpty();

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

        when(repository.findIdentifiersByDivisionId(countryId, divisionId))
                .thenReturn(Uni.createFrom().item(List.of(identifier)));
        final List<AdministrativeDivisionIdentifierResponse> result = service
                .findIdentifiersByDivisionId(countryId.value(), divisionId.value())
                .await().indefinitely();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).schemeCode()).isEqualTo("ISO_3166_2");
    }

    @Test
    void shouldFindNamesByDivisionId() {
        assertThat(service.findNamesByDivisionId(null, divisionId.value()).await().indefinitely()).isEmpty();
        assertThat(service.findNamesByDivisionId(countryId.value(), null).await().indefinitely()).isEmpty();

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

        when(repository.findNamesByDivisionId(countryId, divisionId))
                .thenReturn(Uni.createFrom().item(List.of(name)));
        final List<AdministrativeDivisionNameResponse> result = service
                .findNamesByDivisionId(countryId.value(), divisionId.value())
                .await().indefinitely();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Provincia de Pichincha");
    }
}

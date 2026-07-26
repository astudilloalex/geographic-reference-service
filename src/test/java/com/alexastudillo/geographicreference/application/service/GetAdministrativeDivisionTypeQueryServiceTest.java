package com.alexastudillo.geographicreference.application.service;

import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionTypeResponse;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivisionType;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.AuditInfo;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionTypeId;
import com.alexastudillo.geographicreference.domain.port.output.AdministrativeDivisionTypeRepository;
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
class GetAdministrativeDivisionTypeQueryServiceTest {

    @Mock
    private AdministrativeDivisionTypeRepository repository;

    private GetAdministrativeDivisionTypeQueryService service;

    private final CountryId countryId = CountryId.of(UUID.randomUUID());
    private final DivisionTypeId typeId = DivisionTypeId.of(UUID.randomUUID());
    private final AuditInfo auditInfo = AuditInfo.create("test-user");
    private final AdministrativeDivisionType type = new AdministrativeDivisionType(
            typeId,
            countryId,
            "PROVINCE",
            "Province",
            (short) 1,
            GeographicRecordStatus.ACTIVE,
            auditInfo
    );

    @BeforeEach
    void setUp() {
        service = new GetAdministrativeDivisionTypeQueryService(repository);
    }

    @Test
    void shouldRejectNullRepositoryInConstructor() {
        assertThatThrownBy(() -> new GetAdministrativeDivisionTypeQueryService(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldFindById() {
        assertThat(service.findById(null)).isEmpty();

        when(repository.findById(typeId)).thenReturn(Optional.of(type));
        final Optional<AdministrativeDivisionTypeResponse> result = service.findById(typeId.value());

        assertThat(result).isPresent();
        assertThat(result.get().code()).isEqualTo("PROVINCE");
    }

    @Test
    void shouldFindByCountryIdAndCode() {
        assertThat(service.findByCountryIdAndCode(null, "PROVINCE")).isEmpty();
        assertThat(service.findByCountryIdAndCode(countryId.value(), null)).isEmpty();
        assertThat(service.findByCountryIdAndCode(countryId.value(), " ")).isEmpty();

        when(repository.findByCountryIdAndCode(countryId, "PROVINCE")).thenReturn(Optional.of(type));
        final Optional<AdministrativeDivisionTypeResponse> result = service.findByCountryIdAndCode(countryId.value(), "province");

        assertThat(result).isPresent();
        assertThat(result.get().code()).isEqualTo("PROVINCE");
    }

    @Test
    void shouldListByCountryId() {
        assertThat(service.listByCountryId(null)).isEmpty();

        when(repository.findByCountryId(countryId)).thenReturn(List.of(type));
        final List<AdministrativeDivisionTypeResponse> list = service.listByCountryId(countryId.value());

        assertThat(list).hasSize(1);
        assertThat(list.get(0).code()).isEqualTo("PROVINCE");
    }

    @Test
    void shouldListByCountryIdAndStatus() {
        assertThat(service.listByCountryIdAndStatus(null, "ACTIVE")).isEmpty();
        assertThat(service.listByCountryIdAndStatus(countryId.value(), null)).isEmpty();
        assertThat(service.listByCountryIdAndStatus(countryId.value(), " ")).isEmpty();
        assertThat(service.listByCountryIdAndStatus(countryId.value(), "INVALID")).isEmpty();

        when(repository.findByCountryIdAndStatus(countryId, GeographicRecordStatus.ACTIVE)).thenReturn(List.of(type));
        final List<AdministrativeDivisionTypeResponse> list = service.listByCountryIdAndStatus(countryId.value(), "active");

        assertThat(list).hasSize(1);
        assertThat(list.get(0).status()).isEqualTo("ACTIVE");
    }
}

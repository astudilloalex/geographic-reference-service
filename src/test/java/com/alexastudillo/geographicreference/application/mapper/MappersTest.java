package com.alexastudillo.geographicreference.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionIdentifierResponse;
import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionNameResponse;
import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionResponse;
import com.alexastudillo.geographicreference.application.dto.AdministrativeDivisionTypeResponse;
import com.alexastudillo.geographicreference.application.dto.CountryNameResponse;
import com.alexastudillo.geographicreference.application.dto.CountryNameLookupResponse;
import com.alexastudillo.geographicreference.application.dto.CountryResponse;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivision;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivisionIdentifier;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivisionName;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivisionType;
import com.alexastudillo.geographicreference.domain.model.entity.Country;
import com.alexastudillo.geographicreference.domain.model.entity.CountryName;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicIdentifierStatus;
import com.alexastudillo.geographicreference.domain.model.enums.CountryCodeType;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicNameType;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.domain.model.valobj.Alpha2Code;
import com.alexastudillo.geographicreference.domain.model.valobj.Alpha3Code;
import com.alexastudillo.geographicreference.domain.model.valobj.AuditInfo;
import com.alexastudillo.geographicreference.domain.model.valobj.CountryId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionId;
import com.alexastudillo.geographicreference.domain.model.valobj.DivisionTypeId;
import com.alexastudillo.geographicreference.domain.model.valobj.LanguageTag;
import com.alexastudillo.geographicreference.domain.model.valobj.NumericCode;
import com.alexastudillo.geographicreference.domain.model.valobj.SourceProvenance;
import com.alexastudillo.geographicreference.domain.model.valobj.ValidityPeriod;
import com.alexastudillo.geographicreference.domain.model.projection.CountryNameLookup;

class MappersTest {

    private final CountryId countryId = CountryId.of(UUID.randomUUID());
    private final DivisionTypeId typeId = DivisionTypeId.of(UUID.randomUUID());
    private final DivisionId divisionId = DivisionId.of(UUID.randomUUID());
    private final AuditInfo auditInfo = AuditInfo.create("test-user");
    private final ValidityPeriod validity = ValidityPeriod.unbounded();
    private final SourceProvenance provenance = SourceProvenance.of("ISO", "ref", "rev");

    @Test
    void testCountryApplicationMapper() {
        assertThat(CountryApplicationMapper.toResponse(null)).isNull();
        assertThat(CountryApplicationMapper.toNameResponse(null)).isNull();
        assertThat(CountryApplicationMapper.toNameLookupResponse(null)).isNull();

        final Country country = new Country(
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

        final CountryResponse response = CountryApplicationMapper.toResponse(country);
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(countryId.value());
        assertThat(response.alpha2Code()).isEqualTo("EC");
        assertThat(response.alpha3Code()).isEqualTo("ECU");
        assertThat(response.numericCode()).isEqualTo("218");
        assertThat(response.defaultName()).isEqualTo("Ecuador");
        assertThat(response.officialName()).isEqualTo("Republic of Ecuador");
        assertThat(response.independent()).isTrue();
        assertThat(response.status()).isEqualTo("ACTIVE");

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

        final CountryNameResponse nameResponse = CountryApplicationMapper.toNameResponse(countryName);
        assertThat(nameResponse).isNotNull();
        assertThat(nameResponse.id()).isEqualTo(nameId);
        assertThat(nameResponse.countryId()).isEqualTo(countryId.value());
        assertThat(nameResponse.languageTag()).isEqualTo("es");
        assertThat(nameResponse.nameType()).isEqualTo("OFFICIAL");
        assertThat(nameResponse.name()).isEqualTo("República del Ecuador");
        assertThat(nameResponse.preferred()).isTrue();

        final CountryNameLookup lookup = new CountryNameLookup(
                CountryCodeType.ALPHA2,
                "EC",
                LanguageTag.of("es"),
                GeographicNameType.COMMON,
                "Ecuador",
                true);
        final CountryNameLookupResponse lookupResponse =
                CountryApplicationMapper.toNameLookupResponse(lookup);
        assertThat(lookupResponse.codeType()).isEqualTo("ALPHA2");
        assertThat(lookupResponse.code()).isEqualTo("EC");
        assertThat(lookupResponse.languageTag()).isEqualTo("es");
        assertThat(lookupResponse.nameType()).isEqualTo("COMMON");
        assertThat(lookupResponse.name()).isEqualTo("Ecuador");
        assertThat(lookupResponse.preferred()).isTrue();
    }

    @Test
    void testAdministrativeDivisionTypeApplicationMapper() {
        assertThat(AdministrativeDivisionTypeApplicationMapper.toResponse(null)).isNull();

        final AdministrativeDivisionType type = new AdministrativeDivisionType(
                typeId,
                countryId,
                "PROVINCE",
                "Province",
                (short) 1,
                GeographicRecordStatus.ACTIVE,
                auditInfo);

        final AdministrativeDivisionTypeResponse response = AdministrativeDivisionTypeApplicationMapper
                .toResponse(type);
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(typeId.value());
        assertThat(response.countryId()).isEqualTo(countryId.value());
        assertThat(response.code()).isEqualTo("PROVINCE");
        assertThat(response.name()).isEqualTo("Province");
        assertThat(response.hierarchyLevel()).isEqualTo((short) 1);
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void testAdministrativeDivisionApplicationMapperNullInputs() {
        assertThat(AdministrativeDivisionApplicationMapper.toResponse(null)).isNull();
        assertThat(AdministrativeDivisionApplicationMapper.toIdentifierResponse(null)).isNull();
        assertThat(AdministrativeDivisionApplicationMapper.toNameResponse(null)).isNull();
    }

    @Test
    void testAdministrativeDivisionApplicationMapper() {

        final DivisionId parentId = DivisionId.of(UUID.randomUUID());
        final AdministrativeDivision division = new AdministrativeDivision(
                divisionId,
                countryId,
                typeId,
                parentId,
                "C-QUITO",
                "Quito",
                "Cantón Quito",
                GeographicRecordStatus.ACTIVE,
                validity,
                provenance,
                auditInfo);

        final AdministrativeDivisionResponse response = AdministrativeDivisionApplicationMapper
                .toResponse(division);
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(divisionId.value());
        assertThat(response.countryId()).isEqualTo(countryId.value());
        assertThat(response.divisionTypeId()).isEqualTo(typeId.value());
        assertThat(response.parentDivisionId()).isEqualTo(parentId.value());
        assertThat(response.canonicalCode()).isEqualTo("C-QUITO");
        assertThat(response.defaultName()).isEqualTo("Quito");
        assertThat(response.officialName()).isEqualTo("Cantón Quito");
        assertThat(response.status()).isEqualTo("ACTIVE");

        final UUID identifierId = UUID.randomUUID();
        final AdministrativeDivisionIdentifier identifier = new AdministrativeDivisionIdentifier(
                identifierId,
                countryId,
                divisionId,
                "ISO_3166_2",
                "EC-P",
                true,
                GeographicIdentifierStatus.ACTIVE,
                validity,
                provenance,
                auditInfo);

        final AdministrativeDivisionIdentifierResponse identifierResponse = AdministrativeDivisionApplicationMapper
                .toIdentifierResponse(identifier);
        assertThat(identifierResponse).isNotNull();
        assertThat(identifierResponse.id()).isEqualTo(identifierId);
        assertThat(identifierResponse.countryId()).isEqualTo(countryId.value());
        assertThat(identifierResponse.divisionId()).isEqualTo(divisionId.value());
        assertThat(identifierResponse.schemeCode()).isEqualTo("ISO_3166_2");
        assertThat(identifierResponse.identifierValue()).isEqualTo("EC-P");
        assertThat(identifierResponse.primary()).isTrue();
        assertThat(identifierResponse.status()).isEqualTo("ACTIVE");

        final UUID nameId = UUID.randomUUID();
        final AdministrativeDivisionName name = new AdministrativeDivisionName(
                nameId,
                countryId,
                divisionId,
                LanguageTag.of("es"),
                GeographicNameType.OFFICIAL,
                "Cantón San Francisco de Quito",
                true,
                validity,
                auditInfo);

        final AdministrativeDivisionNameResponse nameResponse = AdministrativeDivisionApplicationMapper
                .toNameResponse(name);
        assertThat(nameResponse).isNotNull();
        assertThat(nameResponse.id()).isEqualTo(nameId);
        assertThat(nameResponse.countryId()).isEqualTo(countryId.value());
        assertThat(nameResponse.divisionId()).isEqualTo(divisionId.value());
        assertThat(nameResponse.languageTag()).isEqualTo("es");
        assertThat(nameResponse.nameType()).isEqualTo("OFFICIAL");
        assertThat(nameResponse.name()).isEqualTo("Cantón San Francisco de Quito");
        assertThat(nameResponse.preferred()).isTrue();
    }
}

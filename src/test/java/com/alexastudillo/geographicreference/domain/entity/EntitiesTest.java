package com.alexastudillo.geographicreference.domain.entity;

import com.alexastudillo.geographicreference.domain.exception.DomainException;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivision;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivisionIdentifier;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivisionName;
import com.alexastudillo.geographicreference.domain.model.entity.AdministrativeDivisionType;
import com.alexastudillo.geographicreference.domain.model.entity.Country;
import com.alexastudillo.geographicreference.domain.model.entity.CountryName;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicIdentifierStatus;
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
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntitiesTest {

    private final CountryId countryId = CountryId.of(UUID.randomUUID());
    private final DivisionTypeId typeId = DivisionTypeId.of(UUID.randomUUID());
    private final DivisionId divisionId = DivisionId.of(UUID.randomUUID());
    private final AuditInfo auditInfo = AuditInfo.create("test-user");
    private final ValidityPeriod validity = ValidityPeriod.unbounded();
    private final SourceProvenance provenance = SourceProvenance.of("ISO");

    @Test
    void testCountryEntity() {
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

        final Country sameId = new Country(
                countryId,
                Alpha2Code.of("US"),
                Alpha3Code.of("USA"),
                NumericCode.of("840"),
                "United States",
                "United States of America",
                true,
                GeographicRecordStatus.ACTIVE,
                validity,
                provenance,
                auditInfo);

        final Country otherId = new Country(
                CountryId.of(UUID.randomUUID()),
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

        assertThat(country)
                .isEqualTo(sameId)
                .isNotEqualTo(otherId);
        assertThat(country.hashCode()).hasSameHashCodeAs(countryId.hashCode());

        final Alpha2Code alpha2 = Alpha2Code.of("EC");
        final Alpha3Code alpha3 = Alpha3Code.of("ECU");
        final NumericCode numeric = NumericCode.of("218");

        assertThatThrownBy(() -> new Country(countryId, alpha2, alpha3, numeric, " ", "Official", true,
                GeographicRecordStatus.ACTIVE, validity, provenance, auditInfo))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new Country(countryId, alpha2, alpha3, numeric, "Default", " ", true,
                GeographicRecordStatus.ACTIVE, validity, provenance, auditInfo))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void testCountryNameEntity() {
        final UUID id = UUID.randomUUID();
        final LanguageTag languageTag = LanguageTag.of("es");
        final CountryName name = new CountryName(
                id,
                countryId,
                languageTag,
                GeographicNameType.OFFICIAL,
                "República del Ecuador",
                true,
                validity,
                auditInfo);

        final CountryName sameId = new CountryName(
                id,
                countryId,
                LanguageTag.of("en"),
                GeographicNameType.COMMON,
                "Ecuador",
                false,
                validity,
                auditInfo);

        final CountryName otherId = new CountryName(
                UUID.randomUUID(),
                countryId,
                languageTag,
                GeographicNameType.OFFICIAL,
                "República del Ecuador",
                true,
                validity,
                auditInfo);

        assertThat(name)
                .isEqualTo(sameId)
                .isNotEqualTo(otherId);
        assertThat(name.hashCode()).hasSameHashCodeAs(id.hashCode());

        assertThatThrownBy(() -> new CountryName(id, countryId, languageTag, GeographicNameType.OFFICIAL, " ",
                true, validity, auditInfo))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void testAdministrativeDivisionTypeEntity() {
        final AdministrativeDivisionType type = new AdministrativeDivisionType(
                typeId,
                countryId,
                "PROVINCE",
                "Province",
                (short) 1,
                GeographicRecordStatus.ACTIVE,
                auditInfo);

        final AdministrativeDivisionType sameId = new AdministrativeDivisionType(
                typeId,
                countryId,
                "CANTON",
                "Canton",
                (short) 2,
                GeographicRecordStatus.ACTIVE,
                auditInfo);

        final AdministrativeDivisionType otherId = new AdministrativeDivisionType(
                DivisionTypeId.of(UUID.randomUUID()),
                countryId,
                "PROVINCE",
                "Province",
                (short) 1,
                GeographicRecordStatus.ACTIVE,
                auditInfo);

        assertThat(type)
                .isEqualTo(sameId)
                .isNotEqualTo(otherId);
        assertThat(type.hashCode()).hasSameHashCodeAs(typeId.hashCode());

        assertThatThrownBy(() -> new AdministrativeDivisionType(typeId, countryId, " ", "Province", (short) 1,
                GeographicRecordStatus.ACTIVE, auditInfo))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new AdministrativeDivisionType(typeId, countryId, "PROVINCE", " ", (short) 1,
                GeographicRecordStatus.ACTIVE, auditInfo))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new AdministrativeDivisionType(typeId, countryId, "PROVINCE", "Province", (short) 0,
                GeographicRecordStatus.ACTIVE, auditInfo))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void testAdministrativeDivisionEntity() {
        final AdministrativeDivision division = new AdministrativeDivision(
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
                auditInfo);

        assertThat(division.isRoot()).isTrue();

        final DivisionId parentId = DivisionId.of(UUID.randomUUID());
        final AdministrativeDivision childDivision = new AdministrativeDivision(
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

        assertThat(childDivision.isRoot()).isFalse();

        final AdministrativeDivision otherId = new AdministrativeDivision(
                DivisionId.of(UUID.randomUUID()),
                countryId,
                typeId,
                null,
                "P-PICHINCHA",
                "Pichincha",
                "Provincia de Pichincha",
                GeographicRecordStatus.ACTIVE,
                validity,
                provenance,
                auditInfo);

        assertThat(division)
                .isEqualTo(childDivision)
                .isNotEqualTo(otherId);
        assertThat(division.hashCode()).hasSameHashCodeAs(divisionId.hashCode());

        assertThatThrownBy(() -> new AdministrativeDivision(divisionId, countryId, typeId, divisionId, "CODE", "Name",
                null, GeographicRecordStatus.ACTIVE, validity, provenance, auditInfo))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new AdministrativeDivision(divisionId, countryId, typeId, null, " ", "Name", null,
                GeographicRecordStatus.ACTIVE, validity, provenance, auditInfo))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new AdministrativeDivision(divisionId, countryId, typeId, null, "CODE", " ", null,
                GeographicRecordStatus.ACTIVE, validity, provenance, auditInfo))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void testAdministrativeDivisionIdentifierEntity() {
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
                auditInfo);

        final AdministrativeDivisionIdentifier sameId = new AdministrativeDivisionIdentifier(
                id,
                countryId,
                divisionId,
                "INEC",
                "17",
                false,
                GeographicIdentifierStatus.ACTIVE,
                validity,
                provenance,
                auditInfo);

        final AdministrativeDivisionIdentifier otherId = new AdministrativeDivisionIdentifier(
                UUID.randomUUID(),
                countryId,
                divisionId,
                "ISO_3166_2",
                "EC-P",
                true,
                GeographicIdentifierStatus.ACTIVE,
                validity,
                provenance,
                auditInfo);

        assertThat(identifier)
                .isEqualTo(sameId)
                .isNotEqualTo(otherId);
        assertThat(identifier.hashCode()).hasSameHashCodeAs(id.hashCode());

        assertThatThrownBy(() -> new AdministrativeDivisionIdentifier(id, countryId, divisionId, " ", "VAL", true,
                GeographicIdentifierStatus.ACTIVE, validity, provenance, auditInfo))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new AdministrativeDivisionIdentifier(id, countryId, divisionId, "SCHEME", " ", true,
                GeographicIdentifierStatus.ACTIVE, validity, provenance, auditInfo))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void testAdministrativeDivisionNameEntity() {
        final UUID id = UUID.randomUUID();
        final LanguageTag languageTag = LanguageTag.of("es");
        final AdministrativeDivisionName name = new AdministrativeDivisionName(
                id,
                countryId,
                divisionId,
                languageTag,
                GeographicNameType.OFFICIAL,
                "Provincia de Pichincha",
                true,
                validity,
                auditInfo);

        final AdministrativeDivisionName sameId = new AdministrativeDivisionName(
                id,
                countryId,
                divisionId,
                LanguageTag.of("en"),
                GeographicNameType.COMMON,
                "Pichincha Province",
                false,
                validity,
                auditInfo);

        final AdministrativeDivisionName otherId = new AdministrativeDivisionName(
                UUID.randomUUID(),
                countryId,
                divisionId,
                languageTag,
                GeographicNameType.OFFICIAL,
                "Provincia de Pichincha",
                true,
                validity,
                auditInfo);

        assertThat(name)
                .isEqualTo(sameId)
                .isNotEqualTo(otherId);
        assertThat(name.hashCode()).hasSameHashCodeAs(id.hashCode());

        assertThatThrownBy(() -> new AdministrativeDivisionName(id, countryId, divisionId, languageTag,
                GeographicNameType.OFFICIAL, " ", true, validity, auditInfo))
                .isInstanceOf(DomainException.class);
    }
}

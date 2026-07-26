package com.alexastudillo.geographicreference.domain.valobj;

import com.alexastudillo.geographicreference.domain.exception.DomainException;
import com.alexastudillo.geographicreference.domain.model.valobj.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValueObjectsTest {

    @Test
    void testCountryId() {
        final UUID uuid = UUID.randomUUID();
        final CountryId id = CountryId.of(uuid);
        assertThat(id.value()).isEqualTo(uuid);
        assertThat(id).hasToString(uuid.toString());

        assertThatThrownBy(() -> new CountryId(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testDivisionTypeId() {
        final UUID uuid = UUID.randomUUID();
        final DivisionTypeId id = DivisionTypeId.of(uuid);
        assertThat(id.value()).isEqualTo(uuid);
        assertThat(id).hasToString(uuid.toString());

        assertThatThrownBy(() -> new DivisionTypeId(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testDivisionId() {
        final UUID uuid = UUID.randomUUID();
        final DivisionId id = DivisionId.of(uuid);
        assertThat(id.value()).isEqualTo(uuid);
        assertThat(id).hasToString(uuid.toString());

        assertThatThrownBy(() -> new DivisionId(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testAlpha2Code() {
        final Alpha2Code code = Alpha2Code.of("EC");
        assertThat(code.value()).isEqualTo("EC");
        assertThat(code).hasToString("EC");

        assertThatThrownBy(() -> new Alpha2Code(null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> Alpha2Code.of("E"))
                .isInstanceOf(DomainException.class);

        assertThatThrownBy(() -> Alpha2Code.of("ECU"))
                .isInstanceOf(DomainException.class);

        assertThatThrownBy(() -> Alpha2Code.of("ec"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void testAlpha3Code() {
        final Alpha3Code code = Alpha3Code.of("ECU");
        assertThat(code.value()).isEqualTo("ECU");
        assertThat(code).hasToString("ECU");

        assertThatThrownBy(() -> new Alpha3Code(null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> Alpha3Code.of("EC"))
                .isInstanceOf(DomainException.class);

        assertThatThrownBy(() -> Alpha3Code.of("ECUA"))
                .isInstanceOf(DomainException.class);

        assertThatThrownBy(() -> Alpha3Code.of("ecu"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void testNumericCode() {
        final NumericCode code = NumericCode.of("218");
        assertThat(code.value()).isEqualTo("218");
        assertThat(code).hasToString("218");

        assertThatThrownBy(() -> new NumericCode(null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> NumericCode.of("21"))
                .isInstanceOf(DomainException.class);

        assertThatThrownBy(() -> NumericCode.of("2189"))
                .isInstanceOf(DomainException.class);

        assertThatThrownBy(() -> NumericCode.of("ABC"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void testLanguageTag() {
        final LanguageTag tag = LanguageTag.of("es-EC");
        assertThat(tag.value()).isEqualTo("es-EC");
        assertThat(tag).hasToString("es-EC");

        assertThatThrownBy(() -> new LanguageTag(null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> LanguageTag.of("  "))
                .isInstanceOf(DomainException.class);

        final String tooLongTag = "a".repeat(36);
        assertThatThrownBy(() -> LanguageTag.of(tooLongTag))
                .isInstanceOf(DomainException.class);

        assertThatThrownBy(() -> LanguageTag.of("invalid_tag#"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void testValidityPeriod() {
        final LocalDate from = LocalDate.of(2020, Month.JANUARY, 1);
        final LocalDate until = LocalDate.of(2025, Month.DECEMBER, 31);
        final ValidityPeriod period = ValidityPeriod.of(from, until);

        assertThat(period.validFrom()).isEqualTo(from);
        assertThat(period.validUntil()).isEqualTo(until);
        assertThat(period.contains(LocalDate.of(2022, Month.JUNE, 15))).isTrue();
        assertThat(period.contains(LocalDate.of(2019, Month.DECEMBER, 31))).isFalse();
        assertThat(period.contains(LocalDate.of(2026, Month.JANUARY, 1))).isFalse();
        assertThat(period.contains(null)).isFalse();

        final ValidityPeriod unbounded = ValidityPeriod.unbounded();
        assertThat(unbounded.validFrom()).isNull();
        assertThat(unbounded.validUntil()).isNull();
        assertThat(unbounded.contains(LocalDate.now())).isTrue();

        assertThatThrownBy(() -> ValidityPeriod.of(until, from))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void testAuditInfo() {
        final AuditInfo audit = AuditInfo.create("admin");
        assertThat(audit.createdBy()).isEqualTo("admin");
        assertThat(audit.updatedBy()).isEqualTo("admin");
        assertThat(audit.version()).isZero();

        final AuditInfo updated = audit.incrementVersion("operator");
        assertThat(updated.updatedBy()).isEqualTo("operator");
        assertThat(updated.version()).isEqualTo(1L);

        final OffsetDateTime now = OffsetDateTime.now(ZoneId.of("UTC"));
        assertThatThrownBy(() -> new AuditInfo(null, "admin", now, "admin", 0L))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AuditInfo(now, "admin", null, "admin", 0L))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AuditInfo(now, "admin", now, null, 0L))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AuditInfo(now, " ", now, "admin", 0L))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new AuditInfo(now, "admin", now, " ", 0L))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new AuditInfo(now, "admin", now, "admin", -1L))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void testSourceProvenance() {
        final SourceProvenance sp = SourceProvenance.of("ISO", "ref-1", "rev-2");
        assertThat(sp.authority()).isEqualTo("ISO");
        assertThat(sp.reference()).isEqualTo("ref-1");
        assertThat(sp.revision()).isEqualTo("rev-2");

        final SourceProvenance spSimple = SourceProvenance.of("ISO");
        assertThat(spSimple.authority()).isEqualTo("ISO");
        assertThat(spSimple.reference()).isNull();
        assertThat(spSimple.revision()).isNull();

        assertThatThrownBy(() -> new SourceProvenance(null, null, null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> SourceProvenance.of(" "))
                .isInstanceOf(DomainException.class);
    }
}

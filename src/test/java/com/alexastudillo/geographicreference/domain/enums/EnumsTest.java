package com.alexastudillo.geographicreference.domain.enums;

import com.alexastudillo.geographicreference.domain.model.enums.GeographicIdentifierStatus;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicNameType;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnumsTest {

    @Test
    void testGeographicRecordStatus() {
        assertThat(GeographicRecordStatus.valueOf("ACTIVE")).isEqualTo(GeographicRecordStatus.ACTIVE);
        assertThat(GeographicRecordStatus.values()).hasSize(4);
    }

    @Test
    void testGeographicNameType() {
        assertThat(GeographicNameType.valueOf("OFFICIAL")).isEqualTo(GeographicNameType.OFFICIAL);
        assertThat(GeographicNameType.values()).hasSize(5);
    }

    @Test
    void testGeographicIdentifierStatus() {
        assertThat(GeographicIdentifierStatus.valueOf("ACTIVE")).isEqualTo(GeographicIdentifierStatus.ACTIVE);
        assertThat(GeographicIdentifierStatus.values()).hasSize(3);
    }
}

package com.alexastudillo.geographicreference.domain.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogUtilTest {

    @Test
    void shouldPrefixAndFormatLogMessages() {
        assertThat(LogUtil.log("COUNTRY REPOSITORY", "Finding country: id=%s", 218))
                .isEqualTo("[COUNTRY REPOSITORY] Finding country: id=218");
        assertThat(LogUtil.log("FILTER", "Completed request   "))
                .isEqualTo("[FILTER] Completed request");
    }
}

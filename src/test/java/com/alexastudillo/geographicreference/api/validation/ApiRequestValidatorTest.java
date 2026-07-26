package com.alexastudillo.geographicreference.api.validation;

import com.alexastudillo.geographicreference.api.error.ApiException;
import com.alexastudillo.geographicreference.api.error.ApiResponseCode;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiRequestValidatorTest {

    private final ApiRequestValidator validator = new ApiRequestValidator();

    @Test
    void shouldNormalizeValidParameters() {
        final String uuid = "00000000-0000-7000-8000-000000000218";

        assertThat(validator.uuid(" " + uuid + " ")).isEqualTo(UUID.fromString(uuid));
        assertThat(validator.alpha2Code(" ec ")).isEqualTo("EC");
        assertThat(validator.alpha3Code(" ecu ")).isEqualTo("ECU");
        assertThat(validator.numericCode(" 218 ")).isEqualTo("218");
        assertThat(validator.code(" province ")).isEqualTo("PROVINCE");
        assertThat(validator.status(" active ")).isEqualTo("ACTIVE");
        assertThat(validator.root("TRUE")).isTrue();
        assertThat(validator.invalidFilters().responseCode())
                .isEqualTo(ApiResponseCode.INVALID_FILTER_COMBINATION);
    }

    @Test
    void shouldRejectMissingAndMalformedValues() {
        assertCode(() -> validator.uuid(null), ApiResponseCode.INVALID_UUID);
        assertCode(() -> validator.uuid("invalid"), ApiResponseCode.INVALID_UUID);
        assertCode(() -> validator.uuid("1-1-1-1-1"), ApiResponseCode.INVALID_UUID);
        assertCode(() -> validator.alpha2Code("E"), ApiResponseCode.BAD_REQUEST);
        assertCode(() -> validator.alpha3Code("EC1"), ApiResponseCode.BAD_REQUEST);
        assertCode(() -> validator.numericCode("21A"), ApiResponseCode.BAD_REQUEST);
        assertCode(() -> validator.code(" "), ApiResponseCode.BAD_REQUEST);
        assertCode(() -> validator.status("unknown"), ApiResponseCode.INVALID_STATUS);
        assertCode(() -> validator.root("false"), ApiResponseCode.INVALID_FILTER_COMBINATION);
    }

    private void assertCode(final Runnable invocation, final ApiResponseCode responseCode) {
        assertThatThrownBy(invocation::run)
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).responseCode())
                .isEqualTo(responseCode);
    }
}

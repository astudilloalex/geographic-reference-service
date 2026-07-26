package com.alexastudillo.geographicreference.infrastructure.api.validation;

import com.alexastudillo.geographicreference.domain.model.enums.GeographicRecordStatus;
import com.alexastudillo.geographicreference.infrastructure.api.error.ApiException;
import com.alexastudillo.geographicreference.infrastructure.api.error.ApiResponseCode;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Validates and normalizes transport-level parameters before invoking
 * application ports.
 */
@ApplicationScoped
public class ApiRequestValidator {

    private static final Pattern ALPHA_2 = Pattern.compile("[A-Z]{2}");
    private static final Pattern ALPHA_3 = Pattern.compile("[A-Z]{3}");
    private static final Pattern NUMERIC = Pattern.compile("\\d{3}");
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    public UUID uuid(final String value) {
        if (value == null || !UUID_PATTERN.matcher(value.trim()).matches()) {
            throw new ApiException(ApiResponseCode.INVALID_UUID);
        }
        return UUID.fromString(value.trim());
    }

    public String alpha2Code(final String value) {
        return matchingCode(value, ALPHA_2);
    }

    public String alpha3Code(final String value) {
        return matchingCode(value, ALPHA_3);
    }

    public String numericCode(final String value) {
        return matchingCode(value, NUMERIC);
    }

    public String code(final String value) {
        return required(value).toUpperCase(Locale.ROOT);
    }

    public String status(final String value) {
        final String normalized = code(value);
        try {
            return GeographicRecordStatus.valueOf(normalized).name();
        } catch (IllegalArgumentException _) {
            throw new ApiException(ApiResponseCode.INVALID_STATUS);
        }
    }

    public boolean root(final String value) {
        if (!"true".equalsIgnoreCase(required(value))) {
            throw new ApiException(ApiResponseCode.INVALID_FILTER_COMBINATION);
        }
        return true;
    }

    public ApiException invalidFilters() {
        return new ApiException(ApiResponseCode.INVALID_FILTER_COMBINATION);
    }

    private String matchingCode(final String value, final Pattern pattern) {
        final String normalized = code(value);
        if (!pattern.matcher(normalized).matches()) {
            throw new ApiException(ApiResponseCode.BAD_REQUEST);
        }
        return normalized;
    }

    private String required(final String value) {
        if (value == null || value.isBlank()) {
            throw new ApiException(ApiResponseCode.BAD_REQUEST);
        }
        return value.trim();
    }
}

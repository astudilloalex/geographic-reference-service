package com.alexastudillo.geographicreference.domain.model.valobj;

import com.alexastudillo.geographicreference.domain.exception.DomainException;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * ISO 3166-1 numeric country code (e.g. {@code 218} for Ecuador).
 *
 * <p>
 * Stored as text to preserve leading zeroes. Validates the invariant
 * {@code ^[0-9]{3}$}.
 */
public record NumericCode(String value) {

    private static final Pattern PATTERN = Pattern.compile("^\\d{3}$");

    public NumericCode {
        Objects.requireNonNull(value, "Numeric code must not be null");
        if (!PATTERN.matcher(value).matches()) {
            throw new DomainException(
                    "Numeric code must be exactly three digits, got: '%s'".formatted(value));
        }
    }

    public static NumericCode of(final String value) {
        return new NumericCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

package com.alexastudillo.geographicreference.domain.model.valobj;

import com.alexastudillo.geographicreference.domain.exception.DomainException;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * ISO 3166-1 alpha-3 country code (e.g. {@code ECU}).
 *
 * <p>Validates the invariant {@code ^[A-Z]{3}$}.
 */
public record Alpha3Code(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[A-Z]{3}$");

    public Alpha3Code {
        Objects.requireNonNull(value, "Alpha-3 code must not be null");
        if (!PATTERN.matcher(value).matches()) {
            throw new DomainException(
                    "Alpha-3 code must be exactly three uppercase letters, got: '%s'".formatted(value)
            );
        }
    }

    public static Alpha3Code of(final String value) {
        return new Alpha3Code(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

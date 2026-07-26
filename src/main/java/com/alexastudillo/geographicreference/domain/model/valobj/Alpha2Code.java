package com.alexastudillo.geographicreference.domain.model.valobj;

import com.alexastudillo.geographicreference.domain.exception.DomainException;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * ISO 3166-1 alpha-2 country code (e.g. {@code EC}).
 *
 * <p>Validates the invariant {@code ^[A-Z]{2}$}.
 */
public record Alpha2Code(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[A-Z]{2}$");

    public Alpha2Code {
        Objects.requireNonNull(value, "Alpha-2 code must not be null");
        if (!PATTERN.matcher(value).matches()) {
            throw new DomainException(
                    "Alpha-2 code must be exactly two uppercase letters, got: '%s'".formatted(value)
            );
        }
    }

    public static Alpha2Code of(final String value) {
        return new Alpha2Code(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

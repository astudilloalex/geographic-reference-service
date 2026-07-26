package com.alexastudillo.geographicreference.domain.model.valobj;

import com.alexastudillo.geographicreference.domain.exception.DomainException;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * BCP 47 language tag (e.g. {@code es}, {@code en}, {@code es-EC}).
 *
 * <p>
 * Maximum length is 35 characters as per the database schema.
 */
public record LanguageTag(String value) {

    private static final int MAX_LENGTH = 35;
    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z]{2,8}(?:-[A-Za-z0-9]{1,8})*+$");

    public LanguageTag {
        Objects.requireNonNull(value, "Language tag must not be null");
        if (value.isBlank()) {
            throw new DomainException("Language tag must not be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new DomainException(
                    "Language tag must not exceed %d characters, got %d".formatted(MAX_LENGTH, value.length()));
        }
        if (!PATTERN.matcher(value).matches()) {
            throw new DomainException(
                    "Language tag does not conform to BCP 47 pattern, got: '%s'".formatted(value));
        }
    }

    public static LanguageTag of(final String value) {
        return new LanguageTag(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

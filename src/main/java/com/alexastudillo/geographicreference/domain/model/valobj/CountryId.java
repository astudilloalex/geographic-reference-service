package com.alexastudillo.geographicreference.domain.model.valobj;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly-typed identity for a {@code Country} aggregate root.
 */
public record CountryId(UUID value) {

    public CountryId {
        Objects.requireNonNull(value, "CountryId value must not be null");
    }

    public static CountryId of(final UUID value) {
        return new CountryId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

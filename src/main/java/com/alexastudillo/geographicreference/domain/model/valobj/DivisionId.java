package com.alexastudillo.geographicreference.domain.model.valobj;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly-typed identity for an {@code AdministrativeDivision} aggregate root.
 */
public record DivisionId(UUID value) {

    public DivisionId {
        Objects.requireNonNull(value, "DivisionId value must not be null");
    }

    public static DivisionId of(final UUID value) {
        return new DivisionId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

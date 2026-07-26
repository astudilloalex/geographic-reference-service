package com.alexastudillo.geographicreference.domain.model.valobj;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly-typed identity for an {@code AdministrativeDivisionType} entity.
 */
public record DivisionTypeId(UUID value) {

    public DivisionTypeId {
        Objects.requireNonNull(value, "DivisionTypeId value must not be null");
    }

    public static DivisionTypeId of(final UUID value) {
        return new DivisionTypeId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

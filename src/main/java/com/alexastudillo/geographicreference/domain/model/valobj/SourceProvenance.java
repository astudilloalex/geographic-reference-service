package com.alexastudillo.geographicreference.domain.model.valobj;

import com.alexastudillo.geographicreference.domain.exception.DomainException;

import java.util.Objects;

/**
 * Provenance metadata tracking the authoritative source of a geographic record.
 *
 * <p>
 * {@code authority} is mandatory (e.g. {@code "ISO"}); {@code reference} and
 * {@code revision} are optional.
 */
public record SourceProvenance(
        String authority,
        String reference,
        String revision) {

    public SourceProvenance {
        Objects.requireNonNull(authority, "Source authority must not be null");
        if (authority.isBlank()) {
            throw new DomainException("Source authority must not be blank");
        }
    }

    public static SourceProvenance of(final String authority,
            final String reference,
            final String revision) {
        return new SourceProvenance(authority, reference, revision);
    }

    public static SourceProvenance of(final String authority) {
        return new SourceProvenance(authority, null, null);
    }
}

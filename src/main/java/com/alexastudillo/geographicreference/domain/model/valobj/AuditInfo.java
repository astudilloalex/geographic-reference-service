package com.alexastudillo.geographicreference.domain.model.valobj;

import com.alexastudillo.geographicreference.domain.exception.DomainException;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Audit metadata carried by every mutable domain entity.
 *
 * <p>
 * Encapsulates creation/update timestamps, actor identifiers, and an
 * optimistic-concurrency version counter.
 */
public record AuditInfo(
        OffsetDateTime createdAt,
        String createdBy,
        OffsetDateTime updatedAt,
        String updatedBy,
        long version) {

    public AuditInfo {
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(createdBy, "createdBy must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        Objects.requireNonNull(updatedBy, "updatedBy must not be null");
        if (createdBy.isBlank()) {
            throw new DomainException("createdBy must not be blank");
        }
        if (updatedBy.isBlank()) {
            throw new DomainException("updatedBy must not be blank");
        }
        if (version < 0) {
            throw new DomainException("version must not be negative, got: %d".formatted(version));
        }
    }

    /**
     * Factory for the initial audit snapshot when an entity is first created.
     */
    public static AuditInfo create(final String actor) {
        final OffsetDateTime now = OffsetDateTime.now(ZoneId.of("UTC"));
        return new AuditInfo(now, actor, now, actor, 0L);
    }

    /**
     * Returns a new {@code AuditInfo} with an incremented version and fresh
     * {@code updatedAt} / {@code updatedBy}.
     */
    public AuditInfo incrementVersion(final String actor) {
        return new AuditInfo(createdAt, createdBy, OffsetDateTime.now(ZoneId.of("UTC")), actor, version + 1);
    }
}

package com.alexastudillo.geographicreference.domain.model.valobj;

import com.alexastudillo.geographicreference.domain.exception.DomainException;

import java.time.LocalDate;

/**
 * Temporal validity window. Both endpoints are optional; when both are present
 * {@code validUntil} must not precede {@code validFrom}.
 */
public record ValidityPeriod(LocalDate validFrom, LocalDate validUntil) {

    public ValidityPeriod {
        if (validFrom != null && validUntil != null && validUntil.isBefore(validFrom)) {
            throw new DomainException(
                    "validUntil (%s) must not be before validFrom (%s)".formatted(validUntil, validFrom));
        }
    }

    public static ValidityPeriod of(final LocalDate validFrom, final LocalDate validUntil) {
        return new ValidityPeriod(validFrom, validUntil);
    }

    /**
     * Creates an unbounded validity period (both endpoints {@code null}).
     */
    public static ValidityPeriod unbounded() {
        return new ValidityPeriod(null, null);
    }

    /**
     * Returns {@code true} if the given date falls within this validity window.
     */
    public boolean contains(final LocalDate date) {
        if (date == null)
            return false;
        final boolean afterStart = validFrom == null || !date.isBefore(validFrom);
        final boolean beforeEnd = validUntil == null || !date.isAfter(validUntil);
        return afterStart && beforeEnd;
    }
}

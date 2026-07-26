package com.alexastudillo.geographicreference.domain.exception;

/**
 * Base unchecked exception for domain invariant violations.
 */
public class DomainException extends RuntimeException {

    public DomainException(final String message) {
        super(message);
    }

    public DomainException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

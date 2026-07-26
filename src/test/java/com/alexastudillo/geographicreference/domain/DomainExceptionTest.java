package com.alexastudillo.geographicreference.domain;

import com.alexastudillo.geographicreference.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainExceptionTest {

    @Test
    void shouldConstructWithMessage() {
        final DomainException ex = new DomainException("Error message");
        assertThat(ex).hasMessage("Error message");
    }

    @Test
    void shouldConstructWithMessageAndCause() {
        final Throwable cause = new RuntimeException("Cause");
        final DomainException ex = new DomainException("Error message", cause);
        assertThat(ex).hasMessage("Error message").hasCause(cause);
    }
}

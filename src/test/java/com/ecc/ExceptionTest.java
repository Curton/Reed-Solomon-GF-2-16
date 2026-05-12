package com.ecc;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExceptionTest {
    @Test
    void decodingException_message() {
        DecodingException ex = new DecodingException("test msg");
        assertEquals("test msg", ex.getMessage());
        assertTrue(ex instanceof ReedSolomonException);
    }

    @Test
    void reedSolomonException_messageOnly() {
        ReedSolomonException ex = new ReedSolomonException("err");
        assertEquals("err", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void reedSolomonException_messageAndCause() {
        Throwable cause = new RuntimeException("root");
        ReedSolomonException ex = new ReedSolomonException("err", cause);
        assertEquals("err", ex.getMessage());
        assertSame(cause, ex.getCause());
    }
}

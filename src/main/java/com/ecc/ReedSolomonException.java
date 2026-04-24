package com.ecc;

public class ReedSolomonException extends RuntimeException {
    public ReedSolomonException(String message) {
        super(message);
    }

    public ReedSolomonException(String message, Throwable cause) {
        super(message, cause);
    }
}

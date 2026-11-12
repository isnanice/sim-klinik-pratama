package com.klinik.exception;

/**
 * Base Exception untuk Sistem Informasi Klinik Pratama.
 */
public class KlinikException extends Exception {
    public KlinikException(String message) {
        super(message);
    }

    public KlinikException(String message, Throwable cause) {
        super(message, cause);
    }
}


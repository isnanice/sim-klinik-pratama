package com.klinik.exception;

/**
 * Dilemparkan jika data masukan tidak memenuhi aturan validasi bisnis (BR-03, BR-04, BR-05, BR-06, dll).
 */
public class DataTidakValidException extends KlinikException {
    private final String fieldName;

    public DataTidakValidException(String message) {
        super(message);
        this.fieldName = null;
    }

    public DataTidakValidException(String fieldName, String message) {
        super(message);
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}


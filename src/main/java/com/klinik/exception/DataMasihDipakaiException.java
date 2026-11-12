package com.klinik.exception;

/**
 * Dilemparkan jika data yang hendak dihapus masih digunakan pada relasi lain (misal memiliki riwayat pemeriksaan, BR-11).
 */
public class DataMasihDipakaiException extends KlinikException {
    private final String entityName;
    private final String entityId;

    public DataMasihDipakaiException(String message) {
        super(message);
        this.entityName = null;
        this.entityId = null;
    }

    public DataMasihDipakaiException(String entityName, String entityId, String message) {
        super(message);
        this.entityName = entityName;
        this.entityId = entityId;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getEntityId() {
        return entityId;
    }
}


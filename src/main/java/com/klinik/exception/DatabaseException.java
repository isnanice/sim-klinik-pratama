package com.klinik.exception;

/**
 * Kegagalan akses database yang bukan kesalahan input pengguna
 * (koneksi putus, konfigurasi db.properties salah, dan sebagainya).
 * Bersifat unchecked; ditangkap oleh view atau penangan terakhir di App agar aplikasi tidak crash.
 */
public class DatabaseException extends KlinikException {
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}

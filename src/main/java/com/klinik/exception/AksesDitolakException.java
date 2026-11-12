package com.klinik.exception;

/**
 * Dilemparkan jika pengguna tidak memiliki hak akses (role) untuk operasi tertentu (misal PETUGAS mengedit Dokter).
 */
public class AksesDitolakException extends KlinikException {
    public AksesDitolakException(String message) {
        super(message);
    }
}


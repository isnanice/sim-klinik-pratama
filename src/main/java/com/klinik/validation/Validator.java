package com.klinik.validation;

import com.klinik.exception.DataTidakValidException;

/**
 * Utilitas validasi umum untuk memastikan aturan bisnis (Business Rules).
 */
public class Validator {

    public static void validateNotEmpty(String value, String fieldName, String errorMessage) throws DataTidakValidException {
        if (value == null || value.trim().isEmpty()) {
            throw new DataTidakValidException(fieldName, errorMessage != null ? errorMessage : fieldName + " tidak boleh kosong.");
        }
    }

    /**
     * BR-04: Nomor telepon diawali '08' atau '+628', diikuti 8-11 digit (total panjang 10-14 karakter).
     */
    public static void validatePhone(String phone) throws DataTidakValidException {
        if (phone == null || phone.trim().isEmpty()) {
            throw new DataTidakValidException("No. Telepon", "Nomor telepon wajib diisi.");
        }
        String clean = phone.trim();
        // Regex: (08\d{8,11}) atau (\+628\d{8,11})
        if (!clean.matches("^(08|\\+628)[0-9]{8,11}$")) {
            throw new DataTidakValidException("No. Telepon", "Format nomor telepon tidak valid. Gunakan awalan 08 atau +628 diikuti 8-11 digit angka.");
        }
    }

    /**
     * BR-06: Tarif atau nominal tidak boleh negatif.
     */
    public static void validateNonNegativeNumber(double number, String fieldName) throws DataTidakValidException {
        if (number < 0) {
            throw new DataTidakValidException(fieldName, fieldName + " tidak boleh bernilai negatif (>= 0).");
        }
    }

    /**
     * Overloading untuk validasi dengan batas minimum kustom.
     */
    public static void validateNonNegativeNumber(double number, double minVal, String fieldName) throws DataTidakValidException {
        if (number < minVal) {
            throw new DataTidakValidException(fieldName, fieldName + " tidak boleh kurang dari " + minVal + ".");
        }
    }
}


package com.klinik.validation;

import com.klinik.exception.DataTidakValidException;
import com.klinik.model.Layanan;

/**
 * Validator khusus entitas Layanan.
 */
public class LayananValidator {

    public static void validate(Layanan layanan) throws DataTidakValidException {
        if (layanan == null) {
            throw new DataTidakValidException("Layanan", "Objek layanan tidak boleh null.");
        }
        Validator.validateNotEmpty(layanan.getNamaLayanan(), "Nama Layanan", "Nama layanan wajib diisi.");
        Validator.validateNonNegativeNumber(layanan.getTarifDasar(), "Tarif Dasar");
    }
}


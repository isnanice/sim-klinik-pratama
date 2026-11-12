package com.klinik.validation;

import com.klinik.exception.DataTidakValidException;
import com.klinik.model.Dokter;

/**
 * Validator khusus entitas Dokter.
 */
public class DokterValidator {

    public static void validate(Dokter dokter) throws DataTidakValidException {
        if (dokter == null) {
            throw new DataTidakValidException("Dokter", "Objek dokter tidak boleh null.");
        }
        Validator.validateNotEmpty(dokter.getNama(), "Nama Dokter", "Nama dokter wajib diisi.");
        if (dokter.getJenisKelamin() == null) {
            throw new DataTidakValidException("Jenis Kelamin", "Jenis kelamin wajib dipilih.");
        }
        Validator.validatePhone(dokter.getNoTelepon());
        Validator.validateNotEmpty(dokter.getNomorRegistrasi(), "Nomor Registrasi", "Nomor registrasi (STR/SIP) wajib diisi.");
        Validator.validateNotEmpty(dokter.getBidangPelayanan(), "Bidang Pelayanan", "Bidang pelayanan dokter wajib diisi.");
    }
}


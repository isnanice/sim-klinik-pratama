package com.klinik.validation;

import java.time.LocalDate;

import com.klinik.exception.DataTidakValidException;
import com.klinik.model.Pasien;

/**
 * Validator khusus entitas Pasien.
 * BR-03 (nama wajib), BR-04 (telepon), BR-05 (tanggal lahir tidak di masa depan),
 * BR-13 (nomor BPJS opsional, jika diisi 13 digit angka).
 */
public class PasienValidator {

    public static void validate(Pasien pasien) throws DataTidakValidException {
        if (pasien == null) {
            throw new DataTidakValidException("Pasien", "Objek pasien tidak boleh null.");
        }
        Validator.validateNotEmpty(pasien.getNama(), "Nama Pasien", "Nama pasien wajib diisi.");
        if (pasien.getJenisKelamin() == null) {
            throw new DataTidakValidException("Jenis Kelamin", "Jenis kelamin wajib dipilih.");
        }
        validateTanggalLahir(pasien.getTanggalLahir());
        Validator.validatePhone(pasien.getNoTelepon());
        validateNoBpjs(pasien.getNoBpjs());
    }

    /** BR-05: tanggal lahir wajib diisi dan tidak boleh di masa depan. */
    public static void validateTanggalLahir(LocalDate tanggalLahir) throws DataTidakValidException {
        if (tanggalLahir == null) {
            throw new DataTidakValidException("Tanggal Lahir", "Tanggal lahir wajib diisi.");
        }
        if (tanggalLahir.isAfter(LocalDate.now())) {
            throw new DataTidakValidException("Tanggal Lahir", "Tanggal lahir tidak boleh di masa depan.");
        }
    }

    /** BR-13: nomor BPJS opsional; jika diisi harus 13 digit angka. */
    public static void validateNoBpjs(String noBpjs) throws DataTidakValidException {
        if (noBpjs == null || noBpjs.trim().isEmpty()) {
            return;
        }
        if (!noBpjs.trim().matches("^[0-9]{13}$")) {
            throw new DataTidakValidException("No. BPJS", "Nomor BPJS harus terdiri dari 13 digit angka (atau kosongkan jika tidak ada).");
        }
    }
}
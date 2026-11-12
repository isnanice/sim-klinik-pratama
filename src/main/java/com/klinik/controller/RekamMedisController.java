package com.klinik.controller;

import com.klinik.dao.PemeriksaanDAO;
import com.klinik.dao.RekamMedisDAO;
import com.klinik.exception.DataNotFoundException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.model.Pemeriksaan;
import com.klinik.model.RekamMedis;
import com.klinik.model.StatusPemeriksaan;
import com.klinik.validation.Validator;

import java.util.List;
import java.util.Optional;

/**
 * Controller untuk mengelola Rekam Medis pasien dan koordinasi pemeriksaan.
 */
public class RekamMedisController {
    private final RekamMedisDAO rekamMedisDAO;
    private final PemeriksaanDAO pemeriksaanDAO;

    public RekamMedisController(RekamMedisDAO rekamMedisDAO, PemeriksaanDAO pemeriksaanDAO) {
        this.rekamMedisDAO = rekamMedisDAO;
        this.pemeriksaanDAO = pemeriksaanDAO;
    }

    public List<RekamMedis> getAllRekamMedis() {
        return rekamMedisDAO.findAll();
    }

    public Optional<RekamMedis> getRekamMedisByPemeriksaanId(String idPemeriksaan) {
        return rekamMedisDAO.findByPemeriksaanId(idPemeriksaan);
    }

    public List<RekamMedis> getRiwayatByPasienId(String idPasien) {
        return rekamMedisDAO.findByPasienId(idPasien);
    }

    /**
     * BR-09: Satu pemeriksaan memiliki satu rekam medis.
     * Jika sudah ada, memperbarui data yang ada; jika belum ada, membuat baru.
     */
    public RekamMedis simpanAtauUbahRekamMedis(RekamMedis rm) throws DataTidakValidException, DataNotFoundException {
        if (rm == null) {
            throw new DataTidakValidException("Rekam Medis", "Objek rekam medis tidak boleh null.");
        }
        if (rm.getPemeriksaan() == null) {
            throw new DataTidakValidException("Pemeriksaan", "Pemeriksaan terkait wajib dipilih.");
        }
        Validator.validateNotEmpty(rm.getDiagnosis(), "Diagnosis", "Diagnosis wajib diisi.");
        Validator.validateNotEmpty(rm.getTindakan(), "Tindakan", "Tindakan medis wajib diisi.");

        // Pastikan pasien dan dokter terisi dari pemeriksaan
        if (rm.getPasien() == null) {
            rm.setPasien(rm.getPemeriksaan().getPasien());
        }
        if (rm.getDokter() == null) {
            rm.setDokter(rm.getPemeriksaan().getDokter());
        }

        Optional<RekamMedis> existing = rekamMedisDAO.findByPemeriksaanId(rm.getPemeriksaan().getId());
        if (existing.isPresent()) {
            rm.setId(existing.get().getId());
            rekamMedisDAO.update(rm);
        } else {
            rekamMedisDAO.insert(rm);
        }

        // Transisi status pemeriksaan jika sedang di fase pemeriksaan dokter
        Pemeriksaan p = rm.getPemeriksaan();
        if (p.getStatus() == StatusPemeriksaan.MENUNGGU_DOKTER) {
            p.setStatus(StatusPemeriksaan.MENUNGGU_OBAT);
            pemeriksaanDAO.update(p);
        }

        return rm;
    }

    public void selesaikanPemeriksaan(String idPemeriksaan) throws DataNotFoundException {
        Pemeriksaan p = pemeriksaanDAO.findById(idPemeriksaan)
                .orElseThrow(() -> new DataNotFoundException("Pemeriksaan dengan ID " + idPemeriksaan + " tidak ditemukan."));
        p.setStatus(StatusPemeriksaan.SELESAI);
        pemeriksaanDAO.update(p);
    }
}


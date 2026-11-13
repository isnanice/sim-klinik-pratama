package com.klinik.controller;

import com.klinik.dao.PasienDAO;
import com.klinik.dao.PemeriksaanDAO;
import com.klinik.exception.DataMasihDipakaiException;
import com.klinik.exception.DataNotFoundException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.model.Pasien;
import com.klinik.validation.PasienValidator;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller untuk mengelola operasi bisnis entitas Pasien.
 * Pasien dikelola oleh ADMIN maupun PETUGAS, sehingga tidak ada pembatasan role di sini.
 */
public class PasienController {
    private final PasienDAO pasienDAO;
    private final PemeriksaanDAO pemeriksaanDAO;

    public PasienController(PasienDAO pasienDAO, PemeriksaanDAO pemeriksaanDAO) {
        this.pasienDAO = pasienDAO;
        this.pemeriksaanDAO = pemeriksaanDAO;
    }

    public List<Pasien> getAllPasien() {
        return pasienDAO.findAll();
    }

    public List<Pasien> getActivePasien() {
        return pasienDAO.findActive();
    }

    public Pasien getPasienById(String id) throws DataNotFoundException {
        return pasienDAO.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Pasien dengan ID " + id + " tidak ditemukan."));
    }

    public void tambahPasien(Pasien pasien) throws DataTidakValidException {
        PasienValidator.validate(pasien);
        normalisasi(pasien);
        pasienDAO.insert(pasien);
    }

    public void ubahPasien(Pasien pasien) throws DataTidakValidException, DataNotFoundException {
        if (pasien.getId() == null || !pasienDAO.findById(pasien.getId()).isPresent()) {
            throw new DataNotFoundException("Pasien yang akan diubah tidak ditemukan.");
        }
        PasienValidator.validate(pasien);
        normalisasi(pasien);
        pasienDAO.update(pasien);
    }

    /**
     * BR-11: Hapus permanen hanya jika pasien belum punya riwayat pemeriksaan.
     * Jika punya, lemparkan DataMasihDipakaiException.
     */
    public void hapusPasien(String id) throws DataMasihDipakaiException, DataNotFoundException {
        Pasien p = getPasienById(id);
        if (pemeriksaanDAO != null && pemeriksaanDAO.existsByPasienId(id)) {
            throw new DataMasihDipakaiException("Pasien", id,
                    "Pasien " + p.getNama() + " (" + id + ") memiliki riwayat pemeriksaan dan tidak dapat dihapus permanen. Gunakan fitur Nonaktifkan.");
        }
        pasienDAO.delete(id);
    }

    public void nonaktifkanPasien(String id, boolean aktif) throws DataNotFoundException {
        getPasienById(id);
        pasienDAO.toggleAktif(id, aktif);
    }

    /**
     * Pencarian pasien. Kriteria: "Semua", "ID", "Nama", atau "No. BPJS".
     * Keyword kosong mengembalikan seluruh pasien.
     */
    public List<Pasien> cariPasien(String keyword, String kriteria) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return pasienDAO.findAll();
        }
        final String kw = keyword.trim().toLowerCase();
        final String k = kriteria == null ? "Semua" : kriteria;
        return pasienDAO.findAll().stream()
                .filter(p -> {
                    boolean cocokId = contains(p.getId(), kw);
                    boolean cocokNama = contains(p.getNama(), kw);
                    boolean cocokBpjs = contains(p.getNoBpjs(), kw);
                    switch (k) {
                        case "ID":
                            return cocokId;
                        case "Nama":
                            return cocokNama;
                        case "No. BPJS":
                            return cocokBpjs;
                        default:
                            return cocokId || cocokNama || cocokBpjs;
                    }
                })
                .collect(Collectors.toList());
    }

    private boolean contains(String source, String lowerKeyword) {
        return source != null && source.toLowerCase().contains(lowerKeyword);
    }

    /** Rapikan spasi dan ubah BPJS kosong menjadi string kosong agar tersimpan konsisten. */
    private void normalisasi(Pasien p) {
        p.setNama(p.getNama().trim());
        p.setNoTelepon(p.getNoTelepon().trim());
        p.setNoBpjs(p.getNoBpjs() == null ? "" : p.getNoBpjs().trim());
        p.setAlamat(p.getAlamat() == null ? "" : p.getAlamat().trim());
    }
}
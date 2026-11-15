package com.klinik.controller;

import com.klinik.dao.PasienDAO;
import com.klinik.dao.PemeriksaanDAO;
import com.klinik.exception.AksesDitolakException;
import com.klinik.exception.DataMasihDipakaiException;
import com.klinik.exception.DataNotFoundException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.model.Pasien;
import com.klinik.util.HakAkses;
import com.klinik.validation.PasienValidator;

import java.util.List;

/**
 * Controller untuk mengelola operasi bisnis entitas Pasien.
 * Menghubungkan PasienView dengan PasienDAO, PasienValidator, dan HakAkses,
 * termasuk penanganan DataMasihDipakaiException dengan opsi menonaktifkan pasien.
 */
public class PasienController {
    private final PasienDAO pasienDAO;
    private final PemeriksaanDAO pemeriksaanDAO;

    public PasienController(PasienDAO pasienDAO, PemeriksaanDAO pemeriksaanDAO) {
        this.pasienDAO = pasienDAO;
        this.pemeriksaanDAO = pemeriksaanDAO;
        if (this.pasienDAO != null && this.pemeriksaanDAO != null) {
            this.pasienDAO.setPemeriksaanDAO(this.pemeriksaanDAO);
        }
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

    public void tambahPasien(Pasien pasien) throws DataTidakValidException, AksesDitolakException {
        HakAkses.periksaAkses(HakAkses.KELOLA_PASIEN);
        PasienValidator.validate(pasien);
        normalisasi(pasien);
        pasienDAO.insert(pasien);
    }

    public void tambah(Pasien pasien) throws DataTidakValidException, AksesDitolakException {
        tambahPasien(pasien);
    }

    public void ubahPasien(Pasien pasien) throws DataTidakValidException, DataNotFoundException, AksesDitolakException {
        HakAkses.periksaAkses(HakAkses.KELOLA_PASIEN);
        if (pasien.getId() == null || !pasienDAO.findById(pasien.getId()).isPresent()) {
            throw new DataNotFoundException("Pasien yang akan diubah tidak ditemukan.");
        }
        PasienValidator.validate(pasien);
        normalisasi(pasien);
        pasienDAO.update(pasien);
    }

    public void ubah(Pasien pasien) throws DataTidakValidException, DataNotFoundException, AksesDitolakException {
        ubahPasien(pasien);
    }

    /**
     * BR-11: Hapus permanen hanya jika pasien belum punya riwayat pemeriksaan.
     * Jika punya, lemparkan DataMasihDipakaiException agar View menawarkan fitur Nonaktifkan.
     */
    public void hapusPasien(String id) throws DataMasihDipakaiException, DataNotFoundException, AksesDitolakException {
        HakAkses.periksaAkses(HakAkses.KELOLA_PASIEN);
        Pasien p = getPasienById(id);
        if ((pemeriksaanDAO != null && pemeriksaanDAO.existsByPasienId(id)) || pasienDAO.hasRiwayatPemeriksaan(id)) {
            throw new DataMasihDipakaiException("Pasien", id,
                    "Pasien " + p.getNama() + " (" + id + ") memiliki riwayat pemeriksaan dan tidak dapat dihapus permanen. Gunakan fitur Nonaktifkan.");
        }
        pasienDAO.delete(id);
    }

    public void hapus(String id) throws DataMasihDipakaiException, DataNotFoundException, AksesDitolakException {
        hapusPasien(id);
    }

    public void nonaktifkanPasien(String id, boolean aktif) throws DataNotFoundException, AksesDitolakException {
        HakAkses.periksaAkses(HakAkses.KELOLA_PASIEN);
        getPasienById(id);
        pasienDAO.toggleAktif(id, aktif);
    }

    public void nonaktifkan(String id) throws DataNotFoundException, AksesDitolakException {
        nonaktifkanPasien(id, false);
    }

    /**
     * Pencarian pasien berdasarkan kata kunci dan kriteria ("Semua", "ID", "NIK", "Nama", "No. BPJS").
     */
    public List<Pasien> cariPasien(String keyword, String kriteria) {
        return pasienDAO.search(keyword, kriteria);
    }

    private void normalisasi(Pasien p) throws DataTidakValidException {
        p.setNama(p.getNama().trim());
        p.setNoTelepon(p.getNoTelepon().trim());
        p.setNoBpjs(p.getNoBpjs() == null ? "" : p.getNoBpjs().trim());
        p.setNik(p.getNik() == null ? "" : p.getNik().trim());
        p.setAlamat(p.getAlamat() == null ? "" : p.getAlamat().trim());
    }
}

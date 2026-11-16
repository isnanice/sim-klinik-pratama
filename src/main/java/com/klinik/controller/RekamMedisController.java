package com.klinik.controller;

import com.klinik.dao.RekamMedisDAO;
import com.klinik.exception.AksesDitolakException;
import com.klinik.exception.DataNotFoundException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.model.Pemeriksaan;
import com.klinik.model.RekamMedis;

import com.klinik.util.HakAkses;

import java.util.List;
import java.util.Optional;

/**
 * Controller rekam medis. Hanya membaca rekam medis dan meneruskan pengisian tiap tahap ke
 * {@link PemeriksaanController}: perubahan status kunjungan tidak boleh dilakukan dari sini
 * (PRD bagian 16), sehingga kontrak untuk view tetap satu pintu.
 */
public class RekamMedisController {
    private final RekamMedisDAO rekamMedisDAO;
    private final PemeriksaanController pemeriksaanController;

    public RekamMedisController(RekamMedisDAO rekamMedisDAO, PemeriksaanController pemeriksaanController) {
        this.rekamMedisDAO = rekamMedisDAO;
        this.pemeriksaanController = pemeriksaanController;
    }

    // ------------------------------------------------------------------ baca (FR-24)

    public List<RekamMedis> getAllRekamMedis() throws AksesDitolakException {
        HakAkses.periksaAkses(HakAkses.LIHAT_REKAM_MEDIS);
        return rekamMedisDAO.findAll();
    }

    public Optional<RekamMedis> getRekamMedisByPemeriksaanId(String idPemeriksaan) throws AksesDitolakException {
        HakAkses.periksaAkses(HakAkses.LIHAT_REKAM_MEDIS);
        return rekamMedisDAO.findByPemeriksaanId(idPemeriksaan);
    }

    /** Riwayat rekam medis satu pasien, terbaru di atas. */
    public List<RekamMedis> riwayatPerPasien(String idPasien) throws AksesDitolakException {
        HakAkses.periksaAkses(HakAkses.LIHAT_REKAM_MEDIS);
        return rekamMedisDAO.findByPasienId(idPasien);
    }

    public List<RekamMedis> getRiwayatByPasienId(String idPasien) throws AksesDitolakException {
        return riwayatPerPasien(idPasien);
    }

    /** Semua pasien (termasuk nonaktif) untuk memilih riwayat di RekamMedisView. */
    public List<com.klinik.model.Pasien> semuaPasien() throws AksesDitolakException {
        return pemeriksaanController.semuaPasien();
    }

    /** Kunjungan yang masih berjalan, untuk pilihan di RekamMedisView. */
    public List<Pemeriksaan> kunjunganBerjalan() throws AksesDitolakException {
        return pemeriksaanController.kunjunganBerjalan();
    }

    // ------------------------------------------------------------------ simpan per tahap (meneruskan)

    public Pemeriksaan simpanTtv(String idPemeriksaan, int sistolik, int diastolik, double tinggiCm, double beratKg)
            throws AksesDitolakException, DataNotFoundException, DataTidakValidException {
        return pemeriksaanController.simpanTtv(idPemeriksaan, sistolik, diastolik, tinggiCm, beratKg);
    }

    public Pemeriksaan simpanPemeriksaanDokter(String idPemeriksaan, String durasiKeluhan, String hasil,
                                               String diagnosis, String resep, String catatan)
            throws AksesDitolakException, DataNotFoundException, DataTidakValidException {
        return pemeriksaanController.simpanPemeriksaanDokter(idPemeriksaan, durasiKeluhan, hasil, diagnosis, resep, catatan);
    }

    public Pemeriksaan simpanFarmasi(String idPemeriksaan, String obatDiberikan)
            throws AksesDitolakException, DataNotFoundException, DataTidakValidException {
        return pemeriksaanController.simpanFarmasi(idPemeriksaan, obatDiberikan);
    }
}

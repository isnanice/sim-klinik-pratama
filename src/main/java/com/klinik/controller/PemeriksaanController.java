package com.klinik.controller;

import com.klinik.dao.DokterDAO;
import com.klinik.dao.LayananDAO;
import com.klinik.dao.PasienDAO;
import com.klinik.dao.PemeriksaanDAO;
import com.klinik.dao.RekamMedisDAO;
import com.klinik.exception.AksesDitolakException;
import com.klinik.exception.DataNotFoundException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.model.Dokter;
import com.klinik.model.JenisPenjamin;
import com.klinik.model.Layanan;
import com.klinik.model.Pasien;
import com.klinik.model.Pemeriksaan;
import com.klinik.model.Petugas;
import com.klinik.model.RekamMedis;
import com.klinik.model.Role;
import com.klinik.model.StatusPemeriksaan;
import com.klinik.util.HakAkses;
import com.klinik.util.SesiPengguna;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Pemilik alur pelayanan: pendaftaran, TTV, pemeriksaan dokter, farmasi, panggil, dan batalkan.
 * SEMUA perubahan status kunjungan lewat kelas ini (PRD bagian 16). Aturan transisi sendiri ada di
 * {@link StatusPemeriksaan} dan {@link Pemeriksaan}; controller memeriksa hak akses, kepemilikan
 * kunjungan oleh dokter, lalu menyerahkan penyimpanan ke DAO (satu transaksi per tahap).
 */
public class PemeriksaanController {
    private final PemeriksaanDAO pemeriksaanDAO;
    private final RekamMedisDAO rekamMedisDAO;
    private final PasienDAO pasienDAO;
    private final DokterDAO dokterDAO;
    private final LayananDAO layananDAO;

    public PemeriksaanController(PemeriksaanDAO pemeriksaanDAO, RekamMedisDAO rekamMedisDAO,
                                 PasienDAO pasienDAO, DokterDAO dokterDAO, LayananDAO layananDAO) {
        this.pemeriksaanDAO = pemeriksaanDAO;
        this.rekamMedisDAO = rekamMedisDAO;
        this.pasienDAO = pasienDAO;
        this.dokterDAO = dokterDAO;
        this.layananDAO = layananDAO;
    }

    // ------------------------------------------------------------------ baca

    /** Seluruh kunjungan (FR-22). Boleh dilihat Admin, Petugas, dan Dokter. */
    public List<Pemeriksaan> semuaKunjungan() throws AksesDitolakException {
        HakAkses.periksaAkses(HakAkses.KELOLA_ANTREAN);
        return pemeriksaanDAO.findAll();
    }

    public List<Pemeriksaan> kunjunganHariIni() throws AksesDitolakException {
        HakAkses.periksaAkses(HakAkses.KELOLA_ANTREAN);
        return pemeriksaanDAO.findByTanggal(LocalDate.now());
    }

    public List<Pemeriksaan> kunjunganPerTahap(StatusPemeriksaan status) throws AksesDitolakException {
        HakAkses.periksaAkses(HakAkses.KELOLA_ANTREAN);
        return status == null ? pemeriksaanDAO.findAll() : pemeriksaanDAO.findByStatus(status);
    }

    public Optional<Pemeriksaan> cariKunjungan(String idPemeriksaan) throws AksesDitolakException {
        HakAkses.periksaAkses(HakAkses.KELOLA_ANTREAN);
        return pemeriksaanDAO.findById(idPemeriksaan);
    }

    /** Kunjungan yang masih berjalan (belum SELESAI atau DIBATALKAN), untuk pilihan di RekamMedisView. */
    public List<Pemeriksaan> kunjunganBerjalan() throws AksesDitolakException {
        HakAkses.periksaAkses(HakAkses.KELOLA_ANTREAN);
        return pemeriksaanDAO.findAll().stream()
                .filter(p -> !p.getStatus().isFinal())
                .collect(Collectors.toList());
    }

    /** Pilihan formulir pendaftaran: hanya data aktif (BR-07). */
    public List<Pasien> pasienAktif() throws AksesDitolakException {
        HakAkses.periksaAkses(HakAkses.DAFTAR_KUNJUNGAN);
        return pasienDAO.findActive();
    }

    public List<Dokter> dokterAktif() throws AksesDitolakException {
        HakAkses.periksaAkses(HakAkses.DAFTAR_KUNJUNGAN);
        return dokterDAO.findActive();
    }

    public List<Layanan> layananAktif() throws AksesDitolakException {
        HakAkses.periksaAkses(HakAkses.DAFTAR_KUNJUNGAN);
        return layananDAO.findActive();
    }

    /** Seluruh pasien (termasuk nonaktif) untuk riwayat rekam medis. */
    public List<Pasien> semuaPasien() throws AksesDitolakException {
        HakAkses.periksaAkses(HakAkses.LIHAT_REKAM_MEDIS);
        return pasienDAO.findAll();
    }

    // ------------------------------------------------------------------ pendaftaran (FR-18)

    /** Pendaftaran kunjungan. Sistem memberi nomor antrean poli dan status TERDAFTAR. */
    public Pemeriksaan daftarkan(String idPasien, String idDokter, String idLayanan, LocalDate tanggal,
                                 String keluhan, String durasiKeluhan, JenisPenjamin penjamin)
            throws AksesDitolakException, DataNotFoundException, DataTidakValidException {
        HakAkses.periksaAkses(HakAkses.DAFTAR_KUNJUNGAN);
        Petugas pendaftar = SesiPengguna.getSession();
        if (pendaftar == null || pendaftar.getId() == null) {
            throw new DataTidakValidException("Sesi", "Sesi login tidak ditemukan. Silakan login ulang.");
        }
        Pasien pasien = pasienDAO.findById(idPasien)
                .orElseThrow(() -> new DataNotFoundException("Pasien dengan ID " + idPasien + " tidak ditemukan."));
        Dokter dokter = dokterDAO.findById(idDokter)
                .orElseThrow(() -> new DataNotFoundException("Dokter dengan ID " + idDokter + " tidak ditemukan."));
        Layanan layanan = layananDAO.findById(idLayanan)
                .orElseThrow(() -> new DataNotFoundException("Layanan dengan ID " + idLayanan + " tidak ditemukan."));

        Pemeriksaan p = new Pemeriksaan();
        p.setPasien(pasien);
        p.setDokter(dokter);
        p.setLayanan(layanan);
        p.setIdPetugas(pendaftar.getId());
        p.setNamaPetugas(pendaftar.getNama());
        p.setTanggalPemeriksaan(tanggal != null ? tanggal : LocalDate.now());
        p.setKeluhanAwal(keluhan);
        p.setDurasiKeluhan(durasiKeluhan);
        p.setJenisPenjamin(penjamin);
        p.validasiPendaftaran();                 // BR-07, BR-16, kelengkapan isian
        pemeriksaanDAO.insert(p);                // ID dan nomor antrean diisi dari database
        return p;
    }

    // ------------------------------------------------------------------ tahap pelayanan

    /** FR-19, tahap 1: petugas mencatat TTV. Rekam medis dibuat dan status menjadi TTV_SELESAI. */
    public Pemeriksaan simpanTtv(String idPemeriksaan, int sistolik, int diastolik, double tinggiCm, double beratKg)
            throws AksesDitolakException, DataNotFoundException, DataTidakValidException {
        HakAkses.periksaAkses(HakAkses.ISI_TTV);
        Pemeriksaan p = muat(idPemeriksaan);
        RekamMedis rm = rekamMedisDAO.findByPemeriksaanId(idPemeriksaan).orElseGet(RekamMedis::new);
        rm.setPemeriksaan(p);
        StatusPemeriksaan dari = p.getStatus();
        rm.isiTtv(sistolik, diastolik, tinggiCm, beratKg);   // BR-09: hanya saat TERDAFTAR
        p.majuKe(StatusPemeriksaan.TTV_SELESAI);
        rekamMedisDAO.simpanTahap(rm, dari, StatusPemeriksaan.TTV_SELESAI);
        return p;
    }

    /** FR-20, tahap 2: dokter pemilik kunjungan mencatat hasil periksa. Status menjadi DIPERIKSA. */
    public Pemeriksaan simpanPemeriksaanDokter(String idPemeriksaan, String durasiKeluhan, String hasil,
                                               String diagnosis, String resep, String catatan)
            throws AksesDitolakException, DataNotFoundException, DataTidakValidException {
        HakAkses.periksaAkses(HakAkses.PERIKSA_DOKTER);
        Pemeriksaan p = muat(idPemeriksaan);
        pastikanDokterPemilik(p);
        RekamMedis rm = rekamMedisDAO.findByPemeriksaanId(idPemeriksaan)
                .orElseThrow(() -> new DataTidakValidException("Rekam Medis",
                        "Pemeriksaan awal (TTV) belum dicatat untuk kunjungan ini."));
        rm.setPemeriksaan(p);
        StatusPemeriksaan dari = p.getStatus();
        rm.isiPemeriksaanDokter(durasiKeluhan, hasil, diagnosis, resep, catatan);   // BR-09: hanya saat TTV_SELESAI
        p.majuKe(StatusPemeriksaan.DIPERIKSA);
        rekamMedisDAO.simpanTahap(rm, dari, StatusPemeriksaan.DIPERIKSA);
        return p;
    }

    /** FR-21, tahap 3: petugas farmasi mencatat obat yang diberikan. Status menjadi SELESAI. */
    public Pemeriksaan simpanFarmasi(String idPemeriksaan, String obatDiberikan)
            throws AksesDitolakException, DataNotFoundException, DataTidakValidException {
        HakAkses.periksaAkses(HakAkses.ISI_FARMASI);
        Pemeriksaan p = muat(idPemeriksaan);
        RekamMedis rm = rekamMedisDAO.findByPemeriksaanId(idPemeriksaan)
                .orElseThrow(() -> new DataTidakValidException("Rekam Medis",
                        "Rekam medis kunjungan ini belum ada."));
        rm.setPemeriksaan(p);
        StatusPemeriksaan dari = p.getStatus();
        rm.isiFarmasi(obatDiberikan);                       // BR-09: hanya saat DIPERIKSA
        p.majuKe(StatusPemeriksaan.SELESAI);
        rekamMedisDAO.simpanTahap(rm, dari, StatusPemeriksaan.SELESAI);
        return p;
    }

    // ------------------------------------------------------------------ panggil dan batalkan

    /** FR-22 dan BR-14: menambah hitungan panggilan pada tahap yang berjalan. */
    public Pemeriksaan panggil(String idPemeriksaan)
            throws AksesDitolakException, DataNotFoundException, DataTidakValidException {
        HakAkses.periksaAkses(HakAkses.KELOLA_ANTREAN);
        Pemeriksaan p = muat(idPemeriksaan);
        pastikanDokterPemilikBilaDokter(p);
        return pemeriksaanDAO.panggilAntrean(idPemeriksaan);
    }

    /** FR-23, BR-15: pasien tidak hadir setelah dipanggil minimal 3 kali. Alasan dan pembatal dicatat. */
    public Pemeriksaan batalkan(String idPemeriksaan, String alasan)
            throws AksesDitolakException, DataNotFoundException, DataTidakValidException {
        HakAkses.periksaAkses(HakAkses.KELOLA_ANTREAN);
        Pemeriksaan p = muat(idPemeriksaan);
        pastikanDokterPemilikBilaDokter(p);
        Petugas pengguna = SesiPengguna.getSession();
        String oleh = pengguna != null ? pengguna.getUsername() : null;
        return pemeriksaanDAO.batalkanAntrean(idPemeriksaan, alasan, oleh);
    }

    // ------------------------------------------------------------------ pendukung

    private Pemeriksaan muat(String idPemeriksaan) throws DataNotFoundException {
        if (idPemeriksaan == null || idPemeriksaan.trim().isEmpty()) {
            throw new DataNotFoundException("Kunjungan belum dipilih.");
        }
        return pemeriksaanDAO.findById(idPemeriksaan.trim())
                .orElseThrow(() -> new DataNotFoundException("Pemeriksaan dengan ID " + idPemeriksaan + " tidak ditemukan."));
    }

    /** Dokter hanya boleh memproses kunjungan dengan id_dokter yang sama dengan akunnya. Admin bebas. */
    private void pastikanDokterPemilik(Pemeriksaan p) throws AksesDitolakException {
        Petugas pengguna = SesiPengguna.getSession();
        if (pengguna == null || pengguna.getRole() == Role.ADMIN) {
            return;
        }
        String idDokterAkun = pengguna.getIdDokter();
        String idDokterKunjungan = p.getDokter() != null ? p.getDokter().getId() : null;
        if (idDokterAkun == null || idDokterAkun.isEmpty() || !idDokterAkun.equalsIgnoreCase(idDokterKunjungan)) {
            throw new AksesDitolakException("Akses ditolak: Anda hanya dapat memproses kunjungan pasien Anda sendiri.");
        }
    }

    private void pastikanDokterPemilikBilaDokter(Pemeriksaan p) throws AksesDitolakException {
        Petugas pengguna = SesiPengguna.getSession();
        if (pengguna != null && pengguna.getRole() == Role.DOKTER) {
            pastikanDokterPemilik(p);
        }
    }
}

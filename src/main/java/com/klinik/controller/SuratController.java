package com.klinik.controller;

import com.klinik.dao.PemeriksaanDAO;
import com.klinik.dao.SuratDAO;
import com.klinik.exception.AksesDitolakException;
import com.klinik.exception.DataNotFoundException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.model.JenisSurat;
import com.klinik.model.Pemeriksaan;
import com.klinik.model.Petugas;
import com.klinik.model.Role;
import com.klinik.model.StatusPemeriksaan;
import com.klinik.model.StatusSurat;
import com.klinik.model.Surat;
import com.klinik.util.HakAkses;
import com.klinik.util.SesiPengguna;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller untuk mengelola pembuatan dan penyerahan Surat Keterangan Medis (Izin Sakit / Rujukan).
 * Menerapkan HakAkses serta aturan bisnis BR-17:
 * - Role DOKTER hanya dapat membuat surat untuk pemeriksaan miliknya sendiri (idDokter sesuai sesi login)
 *   yang berstatus TTV_SELESAI atau DIPERIKSA.
 * - Role PETUGAS / ADMIN dapat melihat daftar surat berstatus DITERUSKAN dan menandainya menjadi DISERAHKAN.
 */
public class SuratController {
    private final SuratDAO suratDAO;
    private final PemeriksaanDAO pemeriksaanDAO;

    public SuratController(SuratDAO suratDAO, PemeriksaanDAO pemeriksaanDAO) {
        this.suratDAO = suratDAO;
        this.pemeriksaanDAO = pemeriksaanDAO;
    }

    public List<Surat> getAllSurat() throws AksesDitolakException {
        HakAkses.periksaAkses(HakAkses.LIHAT_SURAT);
        return suratDAO.findAll();
    }

    public Surat getSuratById(String idSurat) throws DataNotFoundException, AksesDitolakException {
        HakAkses.periksaAkses(HakAkses.LIHAT_SURAT);
        return suratDAO.findById(idSurat)
                .orElseThrow(() -> new DataNotFoundException("Surat dengan ID " + idSurat + " tidak ditemukan."));
    }

    public List<Surat> filterSurat(JenisSurat jenis, StatusSurat status, String keyword) throws AksesDitolakException {
        HakAkses.periksaAkses(HakAkses.LIHAT_SURAT);
        return suratDAO.filterSurat(jenis, status, keyword);
    }

    public List<Surat> getSuratDiteruskan() throws AksesDitolakException {
        HakAkses.periksaAkses(HakAkses.LIHAT_SURAT);
        return suratDAO.findByStatus(StatusSurat.DITERUSKAN);
    }

    /**
     * BR-17: Mengambil daftar pemeriksaan yang layak dibuatkan surat oleh dokter yang sedang login.
     * Hanya pemeriksaan milik idDokter yang sesuai dengan sesi login dan berstatus TTV_SELESAI atau DIPERIKSA.
     */
    public List<Pemeriksaan> getPemeriksaanLayakSurat() throws AksesDitolakException {
        HakAkses.periksaAkses(HakAkses.BUAT_SURAT);
        if (pemeriksaanDAO == null) {
            return Collections.emptyList();
        }
        Petugas current = SesiPengguna.getSession();
        String idDokterLogin = resolveIdDokterLogin(current);

        return pemeriksaanDAO.findAll().stream()
                .filter(p -> p.getStatus() == StatusPemeriksaan.TTV_SELESAI
                        || p.getStatus() == StatusPemeriksaan.DIPERIKSA)
                .filter(p -> {
                    if (current != null && current.getRole() == Role.DOKTER) {
                        return p.getDokter() != null
                                && idDokterLogin != null
                                && idDokterLogin.equalsIgnoreCase(p.getDokter().getId());
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * BR-17: Membuat surat izin sakit atau surat rujukan oleh Dokter untuk pemeriksaan miliknya
     * yang berstatus TTV_SELESAI atau DIPERIKSA. Status awal surat otomatis DITERUSKAN.
     */
    public Surat buatSurat(String idPemeriksaan, JenisSurat jenis, String isi,
                           int jumlahHari, String tujuanRujukan)
            throws AksesDitolakException, DataTidakValidException, DataNotFoundException {
        HakAkses.periksaAkses(HakAkses.BUAT_SURAT);

        if (idPemeriksaan == null || idPemeriksaan.trim().isEmpty()) {
            throw new DataTidakValidException("ID Pemeriksaan", "Pilih data kunjungan/pemeriksaan terlebih dahulu.");
        }

        if (pemeriksaanDAO != null) {
            Pemeriksaan pmr = pemeriksaanDAO.findById(idPemeriksaan.trim())
                    .orElseThrow(() -> new DataNotFoundException("Pemeriksaan dengan ID " + idPemeriksaan + " tidak ditemukan."));

            if (pmr.getStatus() != StatusPemeriksaan.TTV_SELESAI && pmr.getStatus() != StatusPemeriksaan.DIPERIKSA) {
                throw new DataTidakValidException("Status Pemeriksaan",
                        "BR-17: Surat hanya dapat dibuat untuk pemeriksaan yang berstatus TTV_SELESAI atau DIPERIKSA (status saat ini: "
                                + (pmr.getStatus() != null ? pmr.getStatus().getLabel() : "-") + ").");
            }

            Petugas current = SesiPengguna.getSession();
            if (current != null && current.getRole() == Role.DOKTER) {
                String idDokterLogin = resolveIdDokterLogin(current);
                String idDokterPmr = pmr.getDokter() != null ? pmr.getDokter().getId() : "";
                if (idDokterLogin == null || !idDokterLogin.equalsIgnoreCase(idDokterPmr)) {
                    throw new AksesDitolakException(
                            "BR-17: Dokter hanya dapat membuat surat untuk pemeriksaan miliknya sendiri (" + idDokterLogin + ").");
                }
            }
        }

        Surat surat = new Surat();
        surat.setIdPemeriksaan(idPemeriksaan.trim());
        surat.setJenis(jenis);
        surat.setIsi(isi);
        if (jenis == JenisSurat.IZIN) {
            surat.setJumlahHari(jumlahHari);
            surat.setTujuanRujukan("");
        } else if (jenis == JenisSurat.RUJUKAN) {
            surat.setJumlahHari(0);
            surat.setTujuanRujukan(tujuanRujukan);
        }
        surat.setTanggalDibuat(LocalDate.now());
        surat.setStatus(StatusSurat.DITERUSKAN);

        return suratDAO.simpan(surat);
    }

    public Surat buatSurat(Surat surat)
            throws AksesDitolakException, DataTidakValidException, DataNotFoundException {
        if (surat == null) {
            throw new DataTidakValidException("Surat", "Data surat tidak boleh kosong.");
        }
        return buatSurat(
                surat.getIdPemeriksaan(),
                surat.getJenis(),
                surat.getIsi(),
                surat.getJumlahHari(),
                surat.getTujuanRujukan()
        );
    }

    /**
     * Menandai surat berstatus DITERUSKAN menjadi DISERAHKAN oleh Petugas/Admin.
     */
    public Surat tandaiDiserahkan(String idSurat)
            throws AksesDitolakException, DataNotFoundException, DataTidakValidException {
        HakAkses.periksaAkses(HakAkses.SERAHKAN_SURAT);
        Petugas current = SesiPengguna.getSession();
        String diserahkanOleh = current != null && current.getNama() != null && !current.getNama().isEmpty()
                ? current.getNama()
                : (current != null ? current.getUsername() : "Petugas");
        return suratDAO.ubahStatusDiserahkan(idSurat, diserahkanOleh);
    }

    public Surat tandaiDiserahkan(String idSurat, String diserahkanOleh)
            throws AksesDitolakException, DataNotFoundException, DataTidakValidException {
        HakAkses.periksaAkses(HakAkses.SERAHKAN_SURAT);
        return suratDAO.ubahStatusDiserahkan(idSurat, diserahkanOleh);
    }

    public Pemeriksaan getPemeriksaanById(String idPemeriksaan) {
        if (pemeriksaanDAO == null || idPemeriksaan == null) {
            return null;
        }
        return pemeriksaanDAO.findById(idPemeriksaan).orElse(null);
    }

    private String resolveIdDokterLogin(Petugas current) {
        if (current == null) {
            return null;
        }
        if (current.getIdDokter() != null && !current.getIdDokter().trim().isEmpty()) {
            return current.getIdDokter().trim();
        }
        return "DKT-001";
    }
}

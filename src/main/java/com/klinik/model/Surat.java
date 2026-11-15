package com.klinik.model;

import com.klinik.exception.DataTidakValidException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Entitas Surat (Surat Izin Sakit / Surat Rujukan) dalam Sistem Informasi Manajemen Klinik Pratama.
 * Surat yang sudah berstatus DISERAHKAN bersifat final dan tidak dapat diubah lagi.
 */
public class Surat implements DapatDicetak {
    private String idSurat;
    private String idPemeriksaan;
    private JenisSurat jenis;
    private String isi;
    private int jumlahHari;
    private String tujuanRujukan;
    private LocalDate tanggalDibuat;
    private StatusSurat status;
    private String diserahkanOleh;

    public Surat() {
        this.tanggalDibuat = LocalDate.now();
        this.status = StatusSurat.DITERUSKAN;
        this.jumlahHari = 0;
        this.tujuanRujukan = "";
        this.diserahkanOleh = "";
    }

    public Surat(String idSurat, String idPemeriksaan, JenisSurat jenis, String isi,
                 int jumlahHari, String tujuanRujukan, LocalDate tanggalDibuat,
                 StatusSurat status, String diserahkanOleh) throws DataTidakValidException {
        this.idSurat = idSurat != null ? idSurat.trim() : null;
        setIdPemeriksaan(idPemeriksaan);
        setJenis(jenis);
        setIsi(isi);
        if (jenis == JenisSurat.IZIN) {
            setJumlahHari(jumlahHari);
            this.tujuanRujukan = tujuanRujukan != null ? tujuanRujukan.trim() : "";
        } else if (jenis == JenisSurat.RUJUKAN) {
            this.jumlahHari = Math.max(0, jumlahHari);
            setTujuanRujukan(tujuanRujukan);
        }
        this.tanggalDibuat = tanggalDibuat != null ? tanggalDibuat : LocalDate.now();
        this.diserahkanOleh = diserahkanOleh != null ? diserahkanOleh.trim() : "";
        this.status = status != null ? status : StatusSurat.DITERUSKAN;
    }

    private void ensureModifiable() throws DataTidakValidException {
        if (this.status == StatusSurat.DISERAHKAN) {
            throw new DataTidakValidException("Status Surat", "Surat yang sudah berstatus DISERAHKAN tidak dapat diubah.");
        }
    }

    public String getIdSurat() {
        return idSurat;
    }

    public void setIdSurat(String idSurat) throws DataTidakValidException {
        ensureModifiable();
        if (idSurat != null && idSurat.trim().isEmpty()) {
            throw new DataTidakValidException("ID Surat", "ID Surat tidak boleh kosong.");
        }
        this.idSurat = idSurat != null ? idSurat.trim() : null;
    }

    /** Alias untuk kompatibilitas GenericDAO. */
    public String getId() {
        return idSurat;
    }

    public void setId(String id) throws DataTidakValidException {
        setIdSurat(id);
    }

    public String getIdPemeriksaan() {
        return idPemeriksaan;
    }

    public void setIdPemeriksaan(String idPemeriksaan) throws DataTidakValidException {
        ensureModifiable();
        if (idPemeriksaan == null || idPemeriksaan.trim().isEmpty()) {
            throw new DataTidakValidException("ID Pemeriksaan", "ID Pemeriksaan wajib diisi untuk pembuatan surat.");
        }
        this.idPemeriksaan = idPemeriksaan.trim();
    }

    public JenisSurat getJenis() {
        return jenis;
    }

    public void setJenis(JenisSurat jenis) throws DataTidakValidException {
        ensureModifiable();
        if (jenis == null) {
            throw new DataTidakValidException("Jenis Surat", "Jenis surat wajib dipilih (IZIN atau RUJUKAN).");
        }
        this.jenis = jenis;
    }

    public String getIsi() {
        return isi;
    }

    public void setIsi(String isi) throws DataTidakValidException {
        ensureModifiable();
        if (isi == null || isi.trim().isEmpty()) {
            throw new DataTidakValidException("Isi Surat", "Isi/keterangan surat wajib diisi.");
        }
        this.isi = isi.trim();
    }

    public int getJumlahHari() {
        return jumlahHari;
    }

    public void setJumlahHari(int jumlahHari) throws DataTidakValidException {
        ensureModifiable();
        if (this.jenis == JenisSurat.IZIN && jumlahHari < 1) {
            throw new DataTidakValidException("Jumlah Hari", "Jumlah hari izin sakit minimal 1 hari.");
        }
        if (jumlahHari < 0) {
            throw new DataTidakValidException("Jumlah Hari", "Jumlah hari tidak boleh bernilai negatif.");
        }
        this.jumlahHari = jumlahHari;
    }

    public String getTujuanRujukan() {
        return tujuanRujukan;
    }

    public void setTujuanRujukan(String tujuanRujukan) throws DataTidakValidException {
        ensureModifiable();
        if (this.jenis == JenisSurat.RUJUKAN && (tujuanRujukan == null || tujuanRujukan.trim().isEmpty())) {
            throw new DataTidakValidException("Tujuan Rujukan", "Tujuan rujukan wajib diisi untuk surat rujukan.");
        }
        this.tujuanRujukan = tujuanRujukan != null ? tujuanRujukan.trim() : "";
    }

    public LocalDate getTanggalDibuat() {
        return tanggalDibuat;
    }

    public void setTanggalDibuat(LocalDate tanggalDibuat) throws DataTidakValidException {
        ensureModifiable();
        if (tanggalDibuat == null) {
            throw new DataTidakValidException("Tanggal Dibuat", "Tanggal pembuatan surat wajib diisi.");
        }
        this.tanggalDibuat = tanggalDibuat;
    }

    public StatusSurat getStatus() {
        return status;
    }

    public void setStatus(StatusSurat status) throws DataTidakValidException {
        ensureModifiable();
        if (status == null) {
            throw new DataTidakValidException("Status Surat", "Status surat tidak boleh null.");
        }
        this.status = status;
    }

    public String getDiserahkanOleh() {
        return diserahkanOleh;
    }

    public void setDiserahkanOleh(String diserahkanOleh) throws DataTidakValidException {
        ensureModifiable();
        this.diserahkanOleh = diserahkanOleh != null ? diserahkanOleh.trim() : "";
    }

    /**
     * Menandai surat sebagai DISERAHKAN oleh petugas tertentu.
     */
    public void tandaiDiserahkan(String namaAtauUsernamePetugas) throws DataTidakValidException {
        ensureModifiable();
        if (namaAtauUsernamePetugas == null || namaAtauUsernamePetugas.trim().isEmpty()) {
            throw new DataTidakValidException("Petugas Penyerah", "Nama/username petugas yang menyerahkan surat wajib diisi.");
        }
        this.diserahkanOleh = namaAtauUsernamePetugas.trim();
        this.status = StatusSurat.DISERAHKAN;
    }

    /**
     * Memvalidasi kelengkapan seluruh atribut surat berdasarkan jenisnya.
     */
    public void validate() throws DataTidakValidException {
        if (idPemeriksaan == null || idPemeriksaan.trim().isEmpty()) {
            throw new DataTidakValidException("ID Pemeriksaan", "ID Pemeriksaan wajib diisi.");
        }
        if (jenis == null) {
            throw new DataTidakValidException("Jenis Surat", "Jenis surat wajib dipilih.");
        }
        if (isi == null || isi.trim().isEmpty()) {
            throw new DataTidakValidException("Isi Surat", "Isi/keterangan surat wajib diisi.");
        }
        if (jenis == JenisSurat.IZIN && jumlahHari < 1) {
            throw new DataTidakValidException("Jumlah Hari", "Jumlah hari izin sakit minimal 1 hari.");
        }
        if (jenis == JenisSurat.RUJUKAN && (tujuanRujukan == null || tujuanRujukan.trim().isEmpty())) {
            throw new DataTidakValidException("Tujuan Rujukan", "Tujuan rujukan wajib diisi untuk surat rujukan.");
        }
    }

    @Override
    public String cetakRingkasan() {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-18s: %s%n", "ID Surat", idSurat != null ? idSurat : "-"));
        sb.append(String.format("%-18s: %s%n", "ID Pemeriksaan", idPemeriksaan != null ? idPemeriksaan : "-"));
        sb.append(String.format("%-18s: %s%n", "Jenis Surat", jenis != null ? jenis.getLabel() : "-"));
        sb.append(String.format("%-18s: %s%n", "Tanggal Dibuat", tanggalDibuat != null ? tanggalDibuat.format(df) : "-"));
        if (jenis == JenisSurat.IZIN) {
            sb.append(String.format("%-18s: %d Hari%n", "Jumlah Hari Izin", jumlahHari));
        } else if (jenis == JenisSurat.RUJUKAN) {
            sb.append(String.format("%-18s: %s%n", "Tujuan Rujukan", tujuanRujukan != null && !tujuanRujukan.isEmpty() ? tujuanRujukan : "-"));
        }
        sb.append(String.format("%-18s: %s%n", "Isi Keterangan", isi != null ? isi : "-"));
        sb.append(String.format("%-18s: %s%n", "Status", status != null ? status.getLabel() : "-"));
        sb.append(String.format("%-18s: %s%n", "Diserahkan Oleh", diserahkanOleh != null && !diserahkanOleh.isEmpty() ? diserahkanOleh : "-"));
        return sb.toString();
    }

    @Override
    public String toString() {
        return (idSurat != null ? idSurat : "-") + " - " + (jenis != null ? jenis.getLabel() : "-") + " (" + (status != null ? status.getLabel() : "-") + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Surat surat = (Surat) o;
        return Objects.equals(idSurat, surat.idSurat);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idSurat);
    }
}

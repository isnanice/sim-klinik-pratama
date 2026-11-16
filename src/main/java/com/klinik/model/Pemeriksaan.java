package com.klinik.model;

import com.klinik.exception.DataTidakValidException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Kunjungan / pemeriksaan pasien. Satu objek mewakili satu baris tabel pemeriksaan.
 * Data TTV, hasil dokter, dan obat farmasi disimpan di {@link RekamMedis}.
 *
 * Aturan status (BR-08), panggilan (BR-14), dan pembatalan (BR-15) ada di sini,
 * sedangkan hak akses dan transaksi ke database ada di PemeriksaanController / DAO.
 */
public class Pemeriksaan implements DapatDicetak {
    public static final int BATAS_PANGGILAN = 3;

    private String id;
    private int noAntrean;                 // nomor urut per dokter per tanggal, dibuat database
    private Pasien pasien;
    private Dokter dokter;
    private Layanan layanan;
    private String idPetugas;              // petugas pendaftar (FK)
    private String namaPetugas;            // hanya untuk tampilan
    private LocalDate tanggalPemeriksaan;
    private String keluhanAwal;
    private String durasiKeluhan;          // durasi keluhan awal yang dicatat saat pendaftaran
    private JenisPenjamin jenisPenjamin;
    private StatusPemeriksaan status;
    private int jumlahPanggilan;
    private String alasanBatal;
    private String dibatalkanOleh;

    public Pemeriksaan() {
        this.status = StatusPemeriksaan.TERDAFTAR;
        this.tanggalPemeriksaan = LocalDate.now();
        this.jenisPenjamin = JenisPenjamin.NON_BPJS;
        this.jumlahPanggilan = 0;
        this.alasanBatal = "";
        this.dibatalkanOleh = "";
    }

    /** Konstruktor lengkap, dipakai DAO saat membaca dari database. */
    public Pemeriksaan(String id, int noAntrean, Pasien pasien, Dokter dokter, Layanan layanan,
                       String idPetugas, String namaPetugas, LocalDate tanggalPemeriksaan,
                       String keluhanAwal, String durasiKeluhan, JenisPenjamin jenisPenjamin,
                       StatusPemeriksaan status, int jumlahPanggilan, String alasanBatal, String dibatalkanOleh) {
        this.id = id;
        this.noAntrean = Math.max(0, noAntrean);
        this.pasien = pasien;
        this.dokter = dokter;
        this.layanan = layanan;
        this.idPetugas = idPetugas;
        this.namaPetugas = namaPetugas;
        this.tanggalPemeriksaan = tanggalPemeriksaan != null ? tanggalPemeriksaan : LocalDate.now();
        this.keluhanAwal = keluhanAwal;
        this.durasiKeluhan = durasiKeluhan;
        this.jenisPenjamin = jenisPenjamin != null ? jenisPenjamin : JenisPenjamin.NON_BPJS;
        this.status = status != null ? status : StatusPemeriksaan.TERDAFTAR;
        this.jumlahPanggilan = Math.max(0, jumlahPanggilan);
        this.alasanBatal = alasanBatal != null ? alasanBatal : "";
        this.dibatalkanOleh = dibatalkanOleh != null ? dibatalkanOleh : "";
    }

    /** Konstruktor ringkas untuk pendaftaran baru dan pengujian. */
    public Pemeriksaan(String id, Pasien pasien, Dokter dokter, Layanan layanan,
                       String keluhanAwal, String durasiKeluhan, JenisPenjamin jenisPenjamin,
                       StatusPemeriksaan status) {
        this(id, 0, pasien, dokter, layanan, null, null, LocalDate.now(),
                keluhanAwal, durasiKeluhan, jenisPenjamin, status, 0, "", "");
    }

    // ------------------------------------------------------------------ getter dan setter

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getNoAntrean() { return noAntrean; }
    public void setNoAntrean(int noAntrean) { this.noAntrean = Math.max(0, noAntrean); }

    /** Kode antrean untuk tampilan, misalnya P-001. Null bila belum diberi nomor. */
    public String getNomorAntreanPoli() {
        return noAntrean > 0 ? String.format("P-%03d", noAntrean) : null;
    }

    public Pasien getPasien() { return pasien; }
    public void setPasien(Pasien pasien) { this.pasien = pasien; }

    public Dokter getDokter() { return dokter; }
    public void setDokter(Dokter dokter) { this.dokter = dokter; }

    public Layanan getLayanan() { return layanan; }
    public void setLayanan(Layanan layanan) { this.layanan = layanan; }

    public String getIdPetugas() { return idPetugas; }
    public void setIdPetugas(String idPetugas) { this.idPetugas = idPetugas; }

    public String getNamaPetugas() { return namaPetugas; }
    public void setNamaPetugas(String namaPetugas) { this.namaPetugas = namaPetugas; }

    public LocalDate getTanggalPemeriksaan() { return tanggalPemeriksaan; }
    public void setTanggalPemeriksaan(LocalDate tanggalPemeriksaan) { this.tanggalPemeriksaan = tanggalPemeriksaan; }

    public String getKeluhanAwal() { return keluhanAwal; }
    public void setKeluhanAwal(String keluhanAwal) { this.keluhanAwal = keluhanAwal; }

    public String getDurasiKeluhan() { return durasiKeluhan; }
    public void setDurasiKeluhan(String durasiKeluhan) { this.durasiKeluhan = durasiKeluhan; }

    public JenisPenjamin getJenisPenjamin() { return jenisPenjamin; }
    public void setJenisPenjamin(JenisPenjamin jenisPenjamin) { this.jenisPenjamin = jenisPenjamin; }

    public StatusPemeriksaan getStatus() { return status; }

    /**
     * Hanya untuk DAO saat memuat data. Perubahan status oleh aplikasi wajib lewat
     * {@link #majuKe(StatusPemeriksaan)} atau {@link #batalkan(String, String)}.
     */
    public void setStatus(StatusPemeriksaan status) { this.status = status; }

    public int getJumlahPanggilan() { return jumlahPanggilan; }
    public void setJumlahPanggilan(int jumlahPanggilan) { this.jumlahPanggilan = Math.max(0, jumlahPanggilan); }

    public String getAlasanBatal() { return alasanBatal; }
    public void setAlasanBatal(String alasanBatal) { this.alasanBatal = alasanBatal != null ? alasanBatal.trim() : ""; }

    public String getDibatalkanOleh() { return dibatalkanOleh; }
    public void setDibatalkanOleh(String dibatalkanOleh) { this.dibatalkanOleh = dibatalkanOleh != null ? dibatalkanOleh.trim() : ""; }

    // ------------------------------------------------------------------ aturan bisnis

    /** BR-07 (pasien, dokter, layanan aktif), BR-16 (BPJS), dan kelengkapan isian pendaftaran. */
    public void validasiPendaftaran() throws DataTidakValidException {
        if (pasien == null) {
            throw new DataTidakValidException("Pasien", "Pasien wajib dipilih.");
        }
        if (dokter == null) {
            throw new DataTidakValidException("Dokter", "Dokter wajib dipilih.");
        }
        if (layanan == null) {
            throw new DataTidakValidException("Layanan", "Layanan wajib dipilih.");
        }
        if (!pasien.isAktif()) {
            throw new DataTidakValidException("Pasien", "Pasien " + pasien.getNama() + " berstatus nonaktif.");
        }
        if (!dokter.isAktif()) {
            throw new DataTidakValidException("Dokter", "Dokter " + dokter.getNama() + " berstatus nonaktif.");
        }
        if (!layanan.isAktif()) {
            throw new DataTidakValidException("Layanan", "Layanan " + layanan.getNamaLayanan() + " berstatus nonaktif.");
        }
        if (keluhanAwal == null || keluhanAwal.trim().isEmpty()) {
            throw new DataTidakValidException("Keluhan", "Keluhan awal wajib diisi.");
        }
        if (durasiKeluhan == null || durasiKeluhan.trim().isEmpty()) {
            throw new DataTidakValidException("Durasi Keluhan", "Durasi keluhan wajib diisi, misalnya \"3 hari\".");
        }
        if (jenisPenjamin == null) {
            throw new DataTidakValidException("Jenis Penjamin", "Jenis penjamin wajib dipilih.");
        }
        if (jenisPenjamin == JenisPenjamin.BPJS && !pasien.punyaBpjs()) {
            throw new DataTidakValidException("Jenis Penjamin",
                    "Pasien " + pasien.getNama() + " belum memiliki nomor BPJS. Daftarkan sebagai Non-BPJS.");
        }
    }

    /** BR-08: memajukan status. Melempar DataTidakValidException bila melompat atau mundur. */
    public void majuKe(StatusPemeriksaan tujuan) throws DataTidakValidException {
        if (tujuan == null) {
            throw new DataTidakValidException("Status", "Status tujuan tidak boleh kosong.");
        }
        if (status == null || !status.bolehKe(tujuan)) {
            throw new DataTidakValidException("Status",
                    "Status tidak boleh berubah dari " + labelStatus() + " ke " + tujuan.getLabel() + ".");
        }
        this.status = tujuan;
        this.jumlahPanggilan = 0;           // BR-14: hitungan panggilan di-reset setiap status maju
    }

    /** BR-14: menambah hitungan panggilan pada tahap yang sedang berjalan. */
    public void panggil() throws DataTidakValidException {
        if (status == null || status.isFinal()) {
            throw new DataTidakValidException("Status",
                    "Kunjungan berstatus " + labelStatus() + " tidak dapat dipanggil lagi.");
        }
        this.jumlahPanggilan++;
    }

    /** BR-15: sudah cukup dipanggil untuk boleh dibatalkan. */
    public boolean sudahBolehDibatalkan() {
        return status != null && !status.isFinal() && jumlahPanggilan >= BATAS_PANGGILAN;
    }

    /** BR-15: membatalkan kunjungan karena pasien tidak hadir. */
    public void batalkan(String alasan, String oleh) throws DataTidakValidException {
        if (status == StatusPemeriksaan.SELESAI) {
            throw new DataTidakValidException("Status", "Kunjungan yang sudah SELESAI tidak dapat dibatalkan.");
        }
        if (status == StatusPemeriksaan.DIBATALKAN) {
            throw new DataTidakValidException("Status", "Kunjungan ini sudah berstatus DIBATALKAN.");
        }
        if (jumlahPanggilan < BATAS_PANGGILAN) {
            throw new DataTidakValidException("Jumlah Panggilan",
                    "Kunjungan belum dapat dibatalkan: pasien baru dipanggil " + jumlahPanggilan
                            + " kali (minimal " + BATAS_PANGGILAN + " kali).");
        }
        if (alasan == null || alasan.trim().isEmpty()) {
            throw new DataTidakValidException("Alasan Pembatalan", "Alasan pembatalan wajib diisi.");
        }
        if (oleh == null || oleh.trim().isEmpty()) {
            throw new DataTidakValidException("Pembatal", "Pembatal kunjungan tidak diketahui (sesi login hilang).");
        }
        this.alasanBatal = alasan.trim();
        this.dibatalkanOleh = oleh.trim();
        this.status = StatusPemeriksaan.DIBATALKAN;
    }

    private String labelStatus() {
        return status != null ? status.getLabel() : "-";
    }

    // ------------------------------------------------------------------ DapatDicetak dan Object

    @Override
    public String cetakRingkasan() {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-18s: %s%n", "ID Pemeriksaan", id != null ? id : "-"));
        sb.append(String.format("%-18s: %s%n", "Antrean Poli", getNomorAntreanPoli() != null ? getNomorAntreanPoli() : "-"));
        sb.append(String.format("%-18s: %s%n", "Pasien", pasien != null ? pasien.getNama() : "-"));
        sb.append(String.format("%-18s: %s%n", "Dokter", dokter != null ? dokter.getNama() : "-"));
        sb.append(String.format("%-18s: %s%n", "Layanan", layanan != null ? layanan.getNamaLayanan() : "-"));
        sb.append(String.format("%-18s: %s%n", "Penjamin", jenisPenjamin != null ? jenisPenjamin.getLabel() : "-"));
        sb.append(String.format("%-18s: %s%n", "Tanggal", tanggalPemeriksaan != null ? tanggalPemeriksaan.format(df) : "-"));
        sb.append(String.format("%-18s: %s%n", "Keluhan", keluhanAwal != null ? keluhanAwal : "-"));
        sb.append(String.format("%-18s: %s%n", "Durasi Keluhan", durasiKeluhan != null ? durasiKeluhan : "-"));
        sb.append(String.format("%-18s: %s%n", "Status", labelStatus()));
        sb.append(String.format("%-18s: %d kali%n", "Jml Panggilan", jumlahPanggilan));
        if (alasanBatal != null && !alasanBatal.isEmpty()) {
            sb.append(String.format("%-18s: %s%n", "Alasan Batal", alasanBatal));
            sb.append(String.format("%-18s: %s%n", "Dibatalkan Oleh", dibatalkanOleh));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return id + " - " + (pasien != null ? pasien.getNama() : "-") + " (" + labelStatus() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(id, ((Pemeriksaan) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

package com.klinik.model;

import com.klinik.exception.DataTidakValidException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Rekam medis satu kunjungan (satu kunjungan satu rekam medis, BR-09).
 * Diisi bertahap mengikuti alur pelayanan:
 * <ol>
 *   <li>{@link #isiTtv} oleh petugas saat status TERDAFTAR (rekam medis dibuat di tahap ini),</li>
 *   <li>{@link #isiPemeriksaanDokter} oleh dokter saat status TTV_SELESAI,</li>
 *   <li>{@link #isiFarmasi} oleh petugas saat status DIPERIKSA.</li>
 * </ol>
 * Kunjungan DIBATALKAN dan SELESAI tidak dapat diisi lagi. Perubahan status kunjungan
 * dilakukan PemeriksaanController, bukan di kelas ini.
 */
public class RekamMedis implements DapatDicetak {
    private String id;
    private Pemeriksaan pemeriksaan;

    // tahap 1: TTV (petugas)
    private Integer tensiSistolik;
    private Integer tensiDiastolik;
    private Double tinggiBadanCm;
    private Double beratBadanKg;

    // tahap 2: dokter
    private String durasiKeluhan;
    private String hasilPemeriksaan;
    private String diagnosis;
    private String resep;
    private String catatan;

    // tahap 3: farmasi (petugas)
    private String obatDiberikan;

    public RekamMedis() {
    }

    public RekamMedis(Pemeriksaan pemeriksaan) {
        this.pemeriksaan = pemeriksaan;
    }

    // ------------------------------------------------------------------ pengisian bertahap

    /** Tahap 1. Hanya untuk kunjungan berstatus TERDAFTAR. */
    public void isiTtv(int sistolik, int diastolik, double tinggiCm, double beratKg) throws DataTidakValidException {
        pastikanStatus(StatusPemeriksaan.TERDAFTAR, "pemeriksaan awal (TTV)");
        validasiTensi(sistolik, diastolik);
        validasiTinggi(tinggiCm);
        validasiBerat(beratKg);
        this.tensiSistolik = sistolik;
        this.tensiDiastolik = diastolik;
        this.tinggiBadanCm = tinggiCm;
        this.beratBadanKg = beratKg;
    }

    /** Tahap 2. Hanya untuk kunjungan berstatus TTV_SELESAI. */
    public void isiPemeriksaanDokter(String durasi, String hasil, String diagnosis, String resep, String catatan)
            throws DataTidakValidException {
        pastikanStatus(StatusPemeriksaan.TTV_SELESAI, "pemeriksaan dokter");
        wajib(hasil, "Hasil Pemeriksaan");
        wajib(diagnosis, "Diagnosis");
        wajib(resep, "Resep dan penjelasan obat");
        this.durasiKeluhan = bersih(durasi);
        this.hasilPemeriksaan = hasil.trim();
        this.diagnosis = diagnosis.trim();
        this.resep = resep.trim();
        this.catatan = bersih(catatan);
    }

    /** Tahap 3. Hanya untuk kunjungan berstatus DIPERIKSA. */
    public void isiFarmasi(String obatDiberikan) throws DataTidakValidException {
        pastikanStatus(StatusPemeriksaan.DIPERIKSA, "farmasi");
        wajib(obatDiberikan, "Obat yang diberikan");
        this.obatDiberikan = obatDiberikan.trim();
    }

    private void pastikanStatus(StatusPemeriksaan diharapkan, String tahap) throws DataTidakValidException {
        if (pemeriksaan == null) {
            throw new DataTidakValidException("Pemeriksaan", "Kunjungan terkait wajib dipilih.");
        }
        StatusPemeriksaan sekarang = pemeriksaan.getStatus();
        if (sekarang == StatusPemeriksaan.DIBATALKAN) {
            throw new DataTidakValidException("Status", "Kunjungan yang dibatalkan tidak dapat diisi rekam medis.");
        }
        if (sekarang != diharapkan) {
            throw new DataTidakValidException("Status",
                    "Tahap " + tahap + " hanya dapat diisi saat status kunjungan " + diharapkan.getLabel()
                            + " (status sekarang: " + (sekarang != null ? sekarang.getLabel() : "-") + ").");
        }
    }

    private static void wajib(String nilai, String nama) throws DataTidakValidException {
        if (nilai == null || nilai.trim().isEmpty()) {
            throw new DataTidakValidException(nama, nama + " wajib diisi.");
        }
    }

    private static String bersih(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    private static void validasiTensi(int sis, int dia) throws DataTidakValidException {
        if (sis < 50 || sis > 300) {
            throw new DataTidakValidException("Tensi Sistolik", "Tensi sistolik harus antara 50 dan 300 mmHg.");
        }
        if (dia < 30 || dia > 200) {
            throw new DataTidakValidException("Tensi Diastolik", "Tensi diastolik harus antara 30 dan 200 mmHg.");
        }
        if (sis <= dia) {
            throw new DataTidakValidException("Tensi", "Tensi sistolik harus lebih besar dari diastolik.");
        }
    }

    private static void validasiTinggi(double cm) throws DataTidakValidException {
        if (cm < 30 || cm > 250) {
            throw new DataTidakValidException("Tinggi Badan", "Tinggi badan harus antara 30 dan 250 cm.");
        }
    }

    private static void validasiBerat(double kg) throws DataTidakValidException {
        if (kg < 1 || kg > 500) {
            throw new DataTidakValidException("Berat Badan", "Berat badan harus antara 1 dan 500 kg.");
        }
    }

    // ------------------------------------------------------------------ status kelengkapan

    public boolean punyaTtv() {
        return tensiSistolik != null && tensiDiastolik != null && tinggiBadanCm != null && beratBadanKg != null;
    }

    public boolean punyaPemeriksaanDokter() {
        return hasilPemeriksaan != null && diagnosis != null && resep != null;
    }

    public boolean punyaFarmasi() {
        return obatDiberikan != null;
    }

    /** Tensi untuk tampilan, misalnya "120/80 mmHg". */
    public String getTensiTeks() {
        return (tensiSistolik != null && tensiDiastolik != null) ? tensiSistolik + "/" + tensiDiastolik + " mmHg" : "-";
    }

    // ------------------------------------------------------------------ turunan dari kunjungan

    public Pasien getPasien() { return pemeriksaan != null ? pemeriksaan.getPasien() : null; }
    public Dokter getDokter() { return pemeriksaan != null ? pemeriksaan.getDokter() : null; }
    public LocalDate getTanggal() { return pemeriksaan != null ? pemeriksaan.getTanggalPemeriksaan() : null; }

    // ------------------------------------------------------------------ getter dan setter (dipakai DAO)

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Pemeriksaan getPemeriksaan() { return pemeriksaan; }
    public void setPemeriksaan(Pemeriksaan pemeriksaan) { this.pemeriksaan = pemeriksaan; }

    public Integer getTensiSistolik() { return tensiSistolik; }
    public void setTensiSistolik(Integer v) { this.tensiSistolik = v; }

    public Integer getTensiDiastolik() { return tensiDiastolik; }
    public void setTensiDiastolik(Integer v) { this.tensiDiastolik = v; }

    public Double getTinggiBadanCm() { return tinggiBadanCm; }
    public void setTinggiBadanCm(Double v) { this.tinggiBadanCm = v; }

    public Double getBeratBadanKg() { return beratBadanKg; }
    public void setBeratBadanKg(Double v) { this.beratBadanKg = v; }

    public String getDurasiKeluhan() { return durasiKeluhan; }
    public void setDurasiKeluhan(String v) { this.durasiKeluhan = v; }

    public String getHasilPemeriksaan() { return hasilPemeriksaan; }
    public void setHasilPemeriksaan(String v) { this.hasilPemeriksaan = v; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String v) { this.diagnosis = v; }

    public String getResep() { return resep; }
    public void setResep(String v) { this.resep = v; }

    public String getCatatan() { return catatan; }
    public void setCatatan(String v) { this.catatan = v; }

    public String getObatDiberikan() { return obatDiberikan; }
    public void setObatDiberikan(String v) { this.obatDiberikan = v; }

    // ------------------------------------------------------------------ DapatDicetak dan Object

    @Override
    public String cetakRingkasan() {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-18s: %s%n", "ID Rekam Medis", id != null ? id : "-"));
        sb.append(String.format("%-18s: %s%n", "ID Pemeriksaan", pemeriksaan != null ? pemeriksaan.getId() : "-"));
        sb.append(String.format("%-18s: %s%n", "Pasien", getPasien() != null ? getPasien().getNama() : "-"));
        sb.append(String.format("%-18s: %s%n", "Dokter", getDokter() != null ? getDokter().getNama() : "-"));
        sb.append(String.format("%-18s: %s%n", "Tanggal", getTanggal() != null ? getTanggal().format(df) : "-"));
        sb.append(String.format("%-18s: %s%n", "Tensi", getTensiTeks()));
        sb.append(String.format("%-18s: %s%n", "Tinggi / Berat",
                (tinggiBadanCm != null ? tinggiBadanCm + " cm" : "-") + " / " + (beratBadanKg != null ? beratBadanKg + " kg" : "-")));
        sb.append(String.format("%-18s: %s%n", "Durasi Keluhan", durasiKeluhan != null ? durasiKeluhan : "-"));
        sb.append(String.format("%-18s: %s%n", "Hasil Pemeriksaan", hasilPemeriksaan != null ? hasilPemeriksaan : "-"));
        sb.append(String.format("%-18s: %s%n", "Diagnosis", diagnosis != null ? diagnosis : "-"));
        sb.append(String.format("%-18s: %s%n", "Resep", resep != null ? resep : "-"));
        sb.append(String.format("%-18s: %s%n", "Obat Diberikan", obatDiberikan != null ? obatDiberikan : "-"));
        sb.append(String.format("%-18s: %s%n", "Catatan", catatan != null ? catatan : "-"));
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(id, ((RekamMedis) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

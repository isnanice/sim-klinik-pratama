package com.klinik.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Entitas Rekam Medis untuk mencatat diagnosis, tindakan medis, resep obat, dan catatan.
 */
public class RekamMedis implements DapatDicetak {
    private String id;
    private Pemeriksaan pemeriksaan;
    private Pasien pasien;
    private Dokter dokter;
    private LocalDate tanggal;
    private String diagnosis;
    private String tindakan;
    private String resepObat;
    private String catatan;

    public RekamMedis() {
        this.tanggal = LocalDate.now();
    }

    public RekamMedis(String id, Pemeriksaan pemeriksaan, Pasien pasien, Dokter dokter,
                      LocalDate tanggal, String diagnosis, String tindakan, String resepObat, String catatan) {
        this.id = id;
        this.pemeriksaan = pemeriksaan;
        this.pasien = pasien;
        this.dokter = dokter;
        this.tanggal = tanggal != null ? tanggal : LocalDate.now();
        this.diagnosis = diagnosis;
        this.tindakan = tindakan;
        this.resepObat = resepObat;
        this.catatan = catatan;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Pemeriksaan getPemeriksaan() {
        return pemeriksaan;
    }

    public void setPemeriksaan(Pemeriksaan pemeriksaan) {
        this.pemeriksaan = pemeriksaan;
    }

    public Pasien getPasien() {
        return pasien;
    }

    public void setPasien(Pasien pasien) {
        this.pasien = pasien;
    }

    public Dokter getDokter() {
        return dokter;
    }

    public void setDokter(Dokter dokter) {
        this.dokter = dokter;
    }

    public LocalDate getTanggal() {
        return tanggal;
    }

    public void setTanggal(LocalDate tanggal) {
        this.tanggal = tanggal;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getTindakan() {
        return tindakan;
    }

    public void setTindakan(String tindakan) {
        this.tindakan = tindakan;
    }

    public String getResepObat() {
        return resepObat;
    }

    public void setResepObat(String resepObat) {
        this.resepObat = resepObat;
    }

    public String getCatatan() {
        return catatan;
    }

    public void setCatatan(String catatan) {
        this.catatan = catatan;
    }

    @Override
    public String cetakRingkasan() {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-18s: %s%n", "ID Rekam Medis", id != null ? id : "-"));
        sb.append(String.format("%-18s: %s%n", "ID Pemeriksaan", pemeriksaan != null ? pemeriksaan.getId() : "-"));
        sb.append(String.format("%-18s: %s%n", "Pasien", pasien != null ? pasien.getNama() : "-"));
        sb.append(String.format("%-18s: %s%n", "Dokter", dokter != null ? dokter.getNama() : "-"));
        sb.append(String.format("%-18s: %s%n", "Tanggal", tanggal != null ? tanggal.format(df) : "-"));
        sb.append(String.format("%-18s: %s%n", "Diagnosis", diagnosis != null ? diagnosis : "-"));
        sb.append(String.format("%-18s: %s%n", "Tindakan", tindakan != null ? tindakan : "-"));
        sb.append(String.format("%-18s: %s%n", "Resep Obat", resepObat != null ? resepObat : "-"));
        sb.append(String.format("%-18s: %s%n", "Catatan", catatan != null ? catatan : "-"));
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RekamMedis that = (RekamMedis) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}


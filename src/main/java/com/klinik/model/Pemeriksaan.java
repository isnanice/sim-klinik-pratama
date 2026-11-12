package com.klinik.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Entitas Pemeriksaan / Kunjungan dalam Sistem Informasi Manajemen Klinik Pratama.
 */
public class Pemeriksaan implements DapatDicetak {
    private String id;
    private String nomorAntreanPoli;
    private Pasien pasien;
    private Dokter dokter;
    private Layanan layanan;
    private LocalDate tanggalPemeriksaan;
    private String keluhanAwal;
    private String durasiKeluhan;
    private String tensi;
    private Double beratBadan;
    private Double tinggiBadan;
    private String hasilPemeriksaan;
    private String namaPetugas;
    private StatusPemeriksaan status;

    public Pemeriksaan() {
        this.status = StatusPemeriksaan.TERDAFTAR;
        this.tanggalPemeriksaan = LocalDate.now();
    }

    public Pemeriksaan(String id, String nomorAntreanPoli, Pasien pasien, Dokter dokter, Layanan layanan,
                       LocalDate tanggalPemeriksaan, String keluhanAwal, String namaPetugas, StatusPemeriksaan status) {
        this.id = id;
        this.nomorAntreanPoli = nomorAntreanPoli;
        this.pasien = pasien;
        this.dokter = dokter;
        this.layanan = layanan;
        this.tanggalPemeriksaan = tanggalPemeriksaan != null ? tanggalPemeriksaan : LocalDate.now();
        this.keluhanAwal = keluhanAwal;
        this.namaPetugas = namaPetugas;
        this.status = status != null ? status : StatusPemeriksaan.TERDAFTAR;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNomorAntreanPoli() {
        return nomorAntreanPoli;
    }

    public void setNomorAntreanPoli(String nomorAntreanPoli) {
        this.nomorAntreanPoli = nomorAntreanPoli;
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

    public Layanan getLayanan() {
        return layanan;
    }

    public void setLayanan(Layanan layanan) {
        this.layanan = layanan;
    }

    public LocalDate getTanggalPemeriksaan() {
        return tanggalPemeriksaan;
    }

    public void setTanggalPemeriksaan(LocalDate tanggalPemeriksaan) {
        this.tanggalPemeriksaan = tanggalPemeriksaan;
    }

    public String getKeluhanAwal() {
        return keluhanAwal;
    }

    public void setKeluhanAwal(String keluhanAwal) {
        this.keluhanAwal = keluhanAwal;
    }

    public String getDurasiKeluhan() {
        return durasiKeluhan;
    }

    public void setDurasiKeluhan(String durasiKeluhan) {
        this.durasiKeluhan = durasiKeluhan;
    }

    public String getTensi() {
        return tensi;
    }

    public void setTensi(String tensi) {
        this.tensi = tensi;
    }

    public Double getBeratBadan() {
        return beratBadan;
    }

    public void setBeratBadan(Double beratBadan) {
        this.beratBadan = beratBadan;
    }

    public Double getTinggiBadan() {
        return tinggiBadan;
    }

    public void setTinggiBadan(Double tinggiBadan) {
        this.tinggiBadan = tinggiBadan;
    }

    public String getHasilPemeriksaan() {
        return hasilPemeriksaan;
    }

    public void setHasilPemeriksaan(String hasilPemeriksaan) {
        this.hasilPemeriksaan = hasilPemeriksaan;
    }

    public String getNamaPetugas() {
        return namaPetugas;
    }

    public void setNamaPetugas(String namaPetugas) {
        this.namaPetugas = namaPetugas;
    }

    public StatusPemeriksaan getStatus() {
        return status;
    }

    public void setStatus(StatusPemeriksaan status) {
        this.status = status;
    }

    @Override
    public String cetakRingkasan() {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-18s: %s%n", "ID Pemeriksaan", id != null ? id : "-"));
        sb.append(String.format("%-18s: %s%n", "Antrean Poli", nomorAntreanPoli != null ? nomorAntreanPoli : "-"));
        sb.append(String.format("%-18s: %s%n", "Pasien", pasien != null ? pasien.getNama() : "-"));
        sb.append(String.format("%-18s: %s%n", "Dokter", dokter != null ? dokter.getNama() : "-"));
        sb.append(String.format("%-18s: %s%n", "Layanan", layanan != null ? layanan.getNamaLayanan() : "-"));
        sb.append(String.format("%-18s: %s%n", "Tanggal", tanggalPemeriksaan != null ? tanggalPemeriksaan.format(df) : "-"));
        sb.append(String.format("%-18s: %s%n", "Keluhan", keluhanAwal != null ? keluhanAwal : "-"));
        sb.append(String.format("%-18s: %s%n", "Status", status != null ? status.getLabel() : "-"));
        return sb.toString();
    }

    @Override
    public String toString() {
        String pName = pasien != null ? pasien.getNama() : "-";
        return id + " - " + pName + " (" + (status != null ? status.getLabel() : "-") + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pemeriksaan that = (Pemeriksaan) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}


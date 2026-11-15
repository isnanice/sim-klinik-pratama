package com.klinik.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Entitas Pasien dalam Sistem Informasi Manajemen Klinik Pratama.
 * Mewarisi Orang dan mengimplementasikan DapatDicetak.
 * Terintegrasi dengan tabel PostgreSQL 'pasien'.
 */
public class Pasien extends Orang implements DapatDicetak {
    private String nik;
    private LocalDate tanggalLahir;
    private String noBpjs;
    private LocalDate tanggalRegistrasi;

    public Pasien() {
        super();
        this.nik = "";
        this.tanggalRegistrasi = LocalDate.now();
        this.aktif = true;
    }

    public Pasien(String id, String nama, JenisKelamin jenisKelamin, LocalDate tanggalLahir,
                  String alamat, String noTelepon, String noBpjs, LocalDate tanggalRegistrasi, boolean aktif) {
        this(id, "", nama, jenisKelamin, tanggalLahir, alamat, noTelepon, noBpjs, tanggalRegistrasi, aktif);
    }

    public Pasien(String id, String nik, String nama, JenisKelamin jenisKelamin, LocalDate tanggalLahir,
                  String alamat, String noTelepon, String noBpjs, LocalDate tanggalRegistrasi, boolean aktif) {
        super(id, nama, jenisKelamin, alamat, noTelepon, aktif);
        this.nik = nik != null ? nik.trim() : "";
        this.tanggalLahir = tanggalLahir;
        this.noBpjs = noBpjs != null ? noBpjs.trim() : "";
        this.tanggalRegistrasi = tanggalRegistrasi != null ? tanggalRegistrasi : LocalDate.now();
    }

    public String getNik() {
        return nik;
    }

    public void setNik(String nik) {
        this.nik = nik != null ? nik.trim() : "";
    }

    public LocalDate getTanggalLahir() {
        return tanggalLahir;
    }

    public void setTanggalLahir(LocalDate tanggalLahir) {
        this.tanggalLahir = tanggalLahir;
    }

    public String getNoBpjs() {
        return noBpjs;
    }

    public void setNoBpjs(String noBpjs) {
        this.noBpjs = noBpjs != null ? noBpjs.trim() : "";
    }

    public LocalDate getTanggalRegistrasi() {
        return tanggalRegistrasi;
    }

    public void setTanggalRegistrasi(LocalDate tanggalRegistrasi) {
        this.tanggalRegistrasi = tanggalRegistrasi != null ? tanggalRegistrasi : LocalDate.now();
    }

    @Override
    public String getPeran() {
        return "Pasien";
    }

    @Override
    public String cetakRingkasan() {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-18s: %s%n", "ID Pasien", id != null ? id : "-"));
        sb.append(String.format("%-18s: %s%n", "NIK", nik != null && !nik.isEmpty() ? nik : "-"));
        sb.append(String.format("%-18s: %s%n", "Nama", nama != null ? nama : "-"));
        sb.append(String.format("%-18s: %s%n", "Jenis Kelamin", jenisKelamin != null ? jenisKelamin.getLabel() : "-"));
        sb.append(String.format("%-18s: %s%n", "Tgl Lahir", tanggalLahir != null ? tanggalLahir.format(df) : "-"));
        sb.append(String.format("%-18s: %s%n", "No. Telepon", noTelepon != null ? noTelepon : "-"));
        sb.append(String.format("%-18s: %s%n", "No. BPJS", noBpjs != null && !noBpjs.isEmpty() ? noBpjs : "-"));
        sb.append(String.format("%-18s: %s%n", "Alamat", alamat != null && !alamat.isEmpty() ? alamat : "-"));
        sb.append(String.format("%-18s: %s%n", "Tgl Registrasi", tanggalRegistrasi != null ? tanggalRegistrasi.format(df) : "-"));
        sb.append(String.format("%-18s: %s%n", "Status", aktif ? "Aktif" : "Nonaktif"));
        return sb.toString();
    }

    @Override
    public String toString() {
        return id + " - " + nama;
    }
}

package com.klinik.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Entitas Pasien dalam Sistem Informasi Manajemen Klinik Pratama.
 * Mewarisi Orang dan mengimplementasikan DapatDicetak.
 */
public class Pasien extends Orang {
    private LocalDate tanggalLahir;
    private String noBpjs;
    private LocalDate tanggalRegistrasi;

    public Pasien() {
        super();
        this.tanggalRegistrasi = LocalDate.now();
    }

    public Pasien(String id, String nama, JenisKelamin jenisKelamin, LocalDate tanggalLahir,
                  String alamat, String noTelepon, String noBpjs, LocalDate tanggalRegistrasi, boolean aktif) {
        super(id, nama, jenisKelamin, alamat, noTelepon, aktif);
        this.tanggalLahir = tanggalLahir;
        this.noBpjs = noBpjs;
        this.tanggalRegistrasi = tanggalRegistrasi != null ? tanggalRegistrasi : LocalDate.now();
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
        this.noBpjs = noBpjs;
    }

    public LocalDate getTanggalRegistrasi() {
        return tanggalRegistrasi;
    }

    public void setTanggalRegistrasi(LocalDate tanggalRegistrasi) {
        this.tanggalRegistrasi = tanggalRegistrasi;
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
        sb.append(String.format("%-18s: %s%n", "Nama", nama != null ? nama : "-"));
        sb.append(String.format("%-18s: %s%n", "Jenis Kelamin", jenisKelamin != null ? jenisKelamin.getLabel() : "-"));
        sb.append(String.format("%-18s: %s%n", "Tgl Lahir", tanggalLahir != null ? tanggalLahir.format(df) : "-"));
        sb.append(String.format("%-18s: %s%n", "No. Telepon", noTelepon != null ? noTelepon : "-"));
        sb.append(String.format("%-18s: %s%n", "No. BPJS", noBpjs != null && !noBpjs.isEmpty() ? noBpjs : "-"));
        sb.append(String.format("%-18s: %s%n", "Alamat", alamat != null ? alamat : "-"));
        sb.append(String.format("%-18s: %s%n", "Tgl Registrasi", tanggalRegistrasi != null ? tanggalRegistrasi.format(df) : "-"));
        sb.append(String.format("%-18s: %s%n", "Status", aktif ? "Aktif" : "Nonaktif"));
        return sb.toString();
    }

    @Override
    public String toString() {
        return id + " - " + nama;
    }
}

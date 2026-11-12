package com.klinik.model;

/**
 * Entitas Dokter dalam Sistem Informasi Manajemen Klinik Pratama.
 * Mewarisi Orang dan mengimplementasikan DapatDicetak.
 */
public class Dokter extends Orang {
    private String nomorRegistrasi;
    private String bidangPelayanan;

    public Dokter() {
        super();
    }

    public Dokter(String id, String nama, JenisKelamin jenisKelamin, String alamat,
                  String noTelepon, String nomorRegistrasi, String bidangPelayanan, boolean aktif) {
        super(id, nama, jenisKelamin, alamat, noTelepon, aktif);
        this.nomorRegistrasi = nomorRegistrasi;
        this.bidangPelayanan = bidangPelayanan;
    }

    public String getNomorRegistrasi() {
        return nomorRegistrasi;
    }

    public void setNomorRegistrasi(String nomorRegistrasi) {
        this.nomorRegistrasi = nomorRegistrasi;
    }

    public String getBidangPelayanan() {
        return bidangPelayanan;
    }

    public void setBidangPelayanan(String bidangPelayanan) {
        this.bidangPelayanan = bidangPelayanan;
    }

    @Override
    public String getPeran() {
        return "Dokter (" + (bidangPelayanan != null ? bidangPelayanan : "Umum") + ")";
    }

    @Override
    public String cetakRingkasan() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-18s: %s%n", "ID Dokter", id != null ? id : "-"));
        sb.append(String.format("%-18s: %s%n", "Nama", nama != null ? nama : "-"));
        sb.append(String.format("%-18s: %s%n", "Jenis Kelamin", jenisKelamin != null ? jenisKelamin.getLabel() : "-"));
        sb.append(String.format("%-18s: %s%n", "Bidang Pelayanan", bidangPelayanan != null ? bidangPelayanan : "-"));
        sb.append(String.format("%-18s: %s%n", "Nomor Registrasi", nomorRegistrasi != null ? nomorRegistrasi : "-"));
        sb.append(String.format("%-18s: %s%n", "No. Telepon", noTelepon != null ? noTelepon : "-"));
        sb.append(String.format("%-18s: %s%n", "Alamat", alamat != null ? alamat : "-"));
        sb.append(String.format("%-18s: %s%n", "Status", aktif ? "Aktif" : "Nonaktif"));
        return sb.toString();
    }

    @Override
    public String toString() {
        return nama + " (" + bidangPelayanan + ")";
    }
}

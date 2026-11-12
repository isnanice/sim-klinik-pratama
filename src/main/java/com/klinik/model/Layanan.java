package com.klinik.model;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * Kelas abstrak dasar Layanan yang mengimplementasikan LayananMedis dan DapatDicetak.
 */
public abstract class Layanan implements LayananMedis, DapatDicetak {
    protected String id;
    protected String namaLayanan;
    protected double tarifDasar;
    protected String keterangan;
    protected boolean aktif;

    public Layanan() {
        this.aktif = true;
    }

    public Layanan(String id, String namaLayanan, double tarifDasar, String keterangan, boolean aktif) {
        this.id = id;
        this.namaLayanan = namaLayanan;
        this.tarifDasar = tarifDasar;
        this.keterangan = keterangan;
        this.aktif = aktif;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getNamaLayanan() {
        return namaLayanan;
    }

    public void setNamaLayanan(String namaLayanan) {
        this.namaLayanan = namaLayanan;
    }

    @Override
    public double getTarifDasar() {
        return tarifDasar;
    }

    public void setTarifDasar(double tarifDasar) {
        this.tarifDasar = tarifDasar;
    }

    public String getKeterangan() {
        return keterangan;
    }

    public void setKeterangan(String keterangan) {
        this.keterangan = keterangan;
    }

    public boolean isAktif() {
        return aktif;
    }

    public void setAktif(boolean aktif) {
        this.aktif = aktif;
    }

    @Override
    public abstract String getJenisLayanan();

    @Override
    public abstract double hitungTarif(double biayaTambahan);

    public String formatRupiah(double nilai) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("id", "ID"));
        return "Rp " + nf.format(Math.round(nilai));
    }

    @Override
    public String cetakRingkasan() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-18s: %s%n", "ID Layanan", id != null ? id : "-"));
        sb.append(String.format("%-18s: %s%n", "Nama Layanan", namaLayanan != null ? namaLayanan : "-"));
        sb.append(String.format("%-18s: %s%n", "Jenis Layanan", getJenisLayanan()));
        sb.append(String.format("%-18s: %s%n", "Tarif Dasar", formatRupiah(tarifDasar)));
        sb.append(String.format("%-18s: %s%n", "Keterangan", keterangan != null ? keterangan : "-"));
        sb.append(String.format("%-18s: %s%n", "Status", aktif ? "Aktif" : "Nonaktif"));
        return sb.toString();
    }

    @Override
    public String toString() {
        return namaLayanan + " (" + getJenisLayanan() + " - " + formatRupiah(tarifDasar) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Layanan layanan = (Layanan) o;
        return Objects.equals(id, layanan.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}


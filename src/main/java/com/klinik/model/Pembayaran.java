package com.klinik.model;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * Entitas Pembayaran untuk transaksi layanan klinik.
 */
public class Pembayaran implements DapatDicetak {
    private String id;
    private Pemeriksaan pemeriksaan;
    private LocalDate tanggalPembayaran;
    private double biayaTambahan;
    private double totalBiaya;
    private MetodePembayaran metodePembayaran;
    private StatusPembayaran statusPembayaran;

    public Pembayaran() {
        this.tanggalPembayaran = LocalDate.now();
        this.statusPembayaran = StatusPembayaran.BELUM_LUNAS;
        this.metodePembayaran = MetodePembayaran.TUNAI;
    }

    public Pembayaran(String id, Pemeriksaan pemeriksaan, LocalDate tanggalPembayaran,
                      double biayaTambahan, double totalBiaya, MetodePembayaran metodePembayaran,
                      StatusPembayaran statusPembayaran) {
        this.id = id;
        this.pemeriksaan = pemeriksaan;
        this.tanggalPembayaran = tanggalPembayaran != null ? tanggalPembayaran : LocalDate.now();
        this.biayaTambahan = biayaTambahan;
        this.totalBiaya = totalBiaya;
        this.metodePembayaran = metodePembayaran != null ? metodePembayaran : MetodePembayaran.TUNAI;
        this.statusPembayaran = statusPembayaran != null ? statusPembayaran : StatusPembayaran.BELUM_LUNAS;
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

    public LocalDate getTanggalPembayaran() {
        return tanggalPembayaran;
    }

    public void setTanggalPembayaran(LocalDate tanggalPembayaran) {
        this.tanggalPembayaran = tanggalPembayaran;
    }

    public double getBiayaTambahan() {
        return biayaTambahan;
    }

    public void setBiayaTambahan(double biayaTambahan) {
        this.biayaTambahan = biayaTambahan;
    }

    public double getTotalBiaya() {
        return totalBiaya;
    }

    public void setTotalBiaya(double totalBiaya) {
        this.totalBiaya = totalBiaya;
    }

    public MetodePembayaran getMetodePembayaran() {
        return metodePembayaran;
    }

    public void setMetodePembayaran(MetodePembayaran metodePembayaran) {
        this.metodePembayaran = metodePembayaran;
    }

    public StatusPembayaran getStatusPembayaran() {
        return statusPembayaran;
    }

    public void setStatusPembayaran(StatusPembayaran statusPembayaran) {
        this.statusPembayaran = statusPembayaran;
    }

    public static String formatRupiah(double nilai) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("id", "ID"));
        return "Rp " + nf.format(Math.round(nilai));
    }

    @Override
    public String cetakRingkasan() {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String pasien = (pemeriksaan != null && pemeriksaan.getPasien() != null) ? pemeriksaan.getPasien().getNama() : "-";
        String layanan = (pemeriksaan != null && pemeriksaan.getLayanan() != null) ? pemeriksaan.getLayanan().getNamaLayanan() : "-";
        String jenisLayanan = (pemeriksaan != null && pemeriksaan.getLayanan() != null) ? pemeriksaan.getLayanan().getJenisLayanan() : "-";
        double tarifDasar = (pemeriksaan != null && pemeriksaan.getLayanan() != null) ? pemeriksaan.getLayanan().getTarifDasar() : 0.0;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-18s: %s%n", "ID Pembayaran", id != null ? id : "-"));
        sb.append(String.format("%-18s: %s%n", "Tanggal", tanggalPembayaran != null ? tanggalPembayaran.format(df) : "-"));
        sb.append(String.format("%-18s: %s%n", "Pasien", pasien));
        sb.append(String.format("%-18s: %s%n", "Layanan", layanan));
        sb.append(String.format("%-18s: %s%n", "Jenis Layanan", jenisLayanan));
        sb.append(String.format("%-18s: %s%n", "Tarif Dasar", formatRupiah(tarifDasar)));
        sb.append(String.format("%-18s: %s%n", "Biaya Tambahan", formatRupiah(biayaTambahan)));
        sb.append(String.format("%-18s: %s%n", "Total Biaya", formatRupiah(totalBiaya)));
        sb.append(String.format("%-18s: %s%n", "Metode", metodePembayaran != null ? metodePembayaran.getLabel() : "-"));
        sb.append(String.format("%-18s: %s%n", "Status", statusPembayaran != null ? statusPembayaran.getLabel() : "-"));
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pembayaran that = (Pembayaran) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}


package com.klinik.model;

/**
 * Representasi Layanan Spesialis di Klinik Pratama.
 * Polimorfisme: Tarif akhir = Tarif Dasar + Biaya Tambahan + Biaya Konsultasi Spesialis.
 */
public class LayananSpesialis extends Layanan {
    public static final double BIAYA_KONSULTASI_SPESIALIS = 50000.0;
    public static final double DEFAULT_BIAYA_KONSULTASI_SPESIALIS = BIAYA_KONSULTASI_SPESIALIS;
    private double biayaKonsultasiSpesialis;

    public LayananSpesialis() {
        super();
        this.biayaKonsultasiSpesialis = BIAYA_KONSULTASI_SPESIALIS;
    }

    public LayananSpesialis(String id, String namaLayanan, double tarifDasar, String keterangan, boolean aktif) {
        super(id, namaLayanan, tarifDasar, keterangan, aktif);
        this.biayaKonsultasiSpesialis = BIAYA_KONSULTASI_SPESIALIS;
    }

    public LayananSpesialis(String id, String namaLayanan, double tarifDasar, String keterangan,
                            boolean aktif, double biayaKonsultasiSpesialis) {
        super(id, namaLayanan, tarifDasar, keterangan, aktif);
        this.biayaKonsultasiSpesialis = biayaKonsultasiSpesialis;
    }

    public double getBiayaKonsultasiSpesialis() {
        return biayaKonsultasiSpesialis;
    }

    public void setBiayaKonsultasiSpesialis(double biayaKonsultasiSpesialis) {
        this.biayaKonsultasiSpesialis = biayaKonsultasiSpesialis;
    }

    @Override
    public String getJenisLayanan() {
        return "Spesialis";
    }

    @Override
    public double hitungTarif() {
        return this.tarifDasar + this.biayaKonsultasiSpesialis;
    }

    @Override
    public double hitungTarif(double biayaTambahan) {
        return super.hitungTarif(biayaTambahan);
    }

    @Override
    public String rincianTarif() {
        return super.rincianTarif() + " + biaya spesialis " + formatRupiah(biayaKonsultasiSpesialis);
    }

    @Override
    public String cetakRingkasan() {
        StringBuilder sb = new StringBuilder(super.cetakRingkasan());
        sb.append(String.format("%-18s: %s%n", "Biaya Spesialis", formatRupiah(biayaKonsultasiSpesialis)));
        return sb.toString();
    }
}


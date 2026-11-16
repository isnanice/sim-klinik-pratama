package com.klinik.model;

/**
 * Representasi Layanan Umum di Klinik Pratama.
 * Tarif akhir = Tarif Dasar + Biaya Tambahan.
 */
public class LayananUmum extends Layanan {

    public LayananUmum() {
        super();
    }

    public LayananUmum(String id, String namaLayanan, double tarifDasar, String keterangan, boolean aktif) {
        super(id, namaLayanan, tarifDasar, keterangan, aktif);
    }

    @Override
    public String getJenisLayanan() {
        return "Umum";
    }

    @Override
    public double hitungTarif() {
        return this.tarifDasar;
    }

    @Override
    public double hitungTarif(double biayaTambahan) {
        return super.hitungTarif(biayaTambahan);
    }
}


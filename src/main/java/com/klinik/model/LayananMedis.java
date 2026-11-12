package com.klinik.model;

/**
 * Kontrak layanan medis pada sistem klinik.
 */
public interface LayananMedis {
    String getId();
    String getNamaLayanan();
    String getJenisLayanan();
    double getTarifDasar();
    double hitungTarif(double biayaTambahan);
}


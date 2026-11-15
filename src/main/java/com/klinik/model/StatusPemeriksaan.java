package com.klinik.model;

/**
 * Enum status tahapan pemeriksaan/kunjungan pasien di klinik.
 * Menyediakan method bolehKe(...) untuk memvalidasi transisi tahapan status.
 */
public enum StatusPemeriksaan {
    TERDAFTAR("TERDAFTAR"),
    MENUNGGU_TTV("MENUNGGU_TTV"),
    TTV_SELESAI("TTV_SELESAI"),
    MENUNGGU_DOKTER("MENUNGGU_DOKTER"),
    DIPERIKSA("DIPERIKSA"),
    MENUNGGU_OBAT("MENUNGGU_OBAT"),
    MENUNGGU("MENUNGGU"),
    SELESAI("SELESAI"),
    DIBATALKAN("DIBATALKAN");

    private final String label;

    StatusPemeriksaan(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Memeriksa apakah status saat ini diizinkan berpindah ke status tujuan.
     * Status SELESAI dan DIBATALKAN bersifat final.
     */
    public boolean bolehKe(StatusPemeriksaan tujuan) {
        if (tujuan == null || this == tujuan) {
            return false;
        }
        if (this == SELESAI || this == DIBATALKAN) {
            return false;
        }
        if (tujuan == DIBATALKAN) {
            return true;
        }
        switch (this) {
            case TERDAFTAR:
                return tujuan == MENUNGGU_TTV || tujuan == TTV_SELESAI || tujuan == MENUNGGU_DOKTER || tujuan == DIPERIKSA;
            case MENUNGGU_TTV:
                return tujuan == TTV_SELESAI || tujuan == MENUNGGU_DOKTER || tujuan == DIPERIKSA;
            case TTV_SELESAI:
                return tujuan == MENUNGGU_DOKTER || tujuan == DIPERIKSA || tujuan == MENUNGGU_OBAT || tujuan == SELESAI;
            case MENUNGGU_DOKTER:
                return tujuan == DIPERIKSA || tujuan == MENUNGGU_OBAT || tujuan == SELESAI;
            case DIPERIKSA:
                return tujuan == MENUNGGU_OBAT || tujuan == SELESAI;
            case MENUNGGU_OBAT:
            case MENUNGGU:
                return tujuan == SELESAI;
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return label;
    }

    public static StatusPemeriksaan fromString(String text) {
        if (text == null) return null;
        for (StatusPemeriksaan s : values()) {
            if (s.name().equalsIgnoreCase(text.trim()) || s.label.equalsIgnoreCase(text.trim())) {
                return s;
            }
        }
        return null;
    }
}

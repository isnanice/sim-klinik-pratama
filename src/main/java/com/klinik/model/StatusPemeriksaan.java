package com.klinik.model;

public enum StatusPemeriksaan {
    TERDAFTAR("TERDAFTAR"),
    MENUNGGU_TTV("MENUNGGU_TTV"),
    MENUNGGU_DOKTER("MENUNGGU_DOKTER"),
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


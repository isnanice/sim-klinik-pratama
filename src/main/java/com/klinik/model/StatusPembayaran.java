package com.klinik.model;

public enum StatusPembayaran {
    BELUM_LUNAS("BELUM_LUNAS"),
    LUNAS("LUNAS");

    private final String label;

    StatusPembayaran(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    public static StatusPembayaran fromString(String text) {
        if (text == null) return null;
        for (StatusPembayaran s : values()) {
            if (s.name().equalsIgnoreCase(text.trim()) || s.label.equalsIgnoreCase(text.trim())) {
                return s;
            }
        }
        return null;
    }
}


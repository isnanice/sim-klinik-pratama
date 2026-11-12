package com.klinik.model;

public enum MetodePembayaran {
    TUNAI("Tunai"),
    TRANSFER("Transfer");

    private final String label;

    MetodePembayaran(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    public static MetodePembayaran fromString(String text) {
        if (text == null) return null;
        for (MetodePembayaran m : values()) {
            if (m.name().equalsIgnoreCase(text.trim()) || m.label.equalsIgnoreCase(text.trim())) {
                return m;
            }
        }
        return null;
    }
}


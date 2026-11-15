package com.klinik.model;

/**
 * Enum peran pengguna dalam Sistem Informasi Manajemen Klinik Pratama.
 */
public enum Role {
    ADMIN("ADMIN"),
    PETUGAS("PETUGAS"),
    DOKTER("DOKTER");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    public static Role fromString(String text) {
        if (text == null) return null;
        for (Role r : values()) {
            if (r.name().equalsIgnoreCase(text.trim()) || r.label.equalsIgnoreCase(text.trim())) {
                return r;
            }
        }
        return null;
    }
}

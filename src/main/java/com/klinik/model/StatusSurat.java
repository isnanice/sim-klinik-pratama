package com.klinik.model;

/**
 * Enum status pemrosesan surat keterangan medis.
 */
public enum StatusSurat {
    DITERUSKAN("DITERUSKAN"),
    DISERAHKAN("DISERAHKAN");

    private final String label;

    StatusSurat(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    public static StatusSurat fromString(String text) {
        if (text == null) return null;
        for (StatusSurat s : values()) {
            if (s.name().equalsIgnoreCase(text.trim()) || s.label.equalsIgnoreCase(text.trim())) {
                return s;
            }
        }
        return null;
    }
}

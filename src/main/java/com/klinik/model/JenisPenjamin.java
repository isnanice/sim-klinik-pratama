package com.klinik.model;

/** Jenis penjamin kunjungan (BR-16). Nilai name() sama dengan kolom jenis_penjamin di database. */
public enum JenisPenjamin {
    BPJS("BPJS"),
    NON_BPJS("Non-BPJS");

    private final String label;

    JenisPenjamin(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    public static JenisPenjamin fromString(String text) {
        if (text == null) return null;
        String t = text.trim();
        for (JenisPenjamin j : values()) {
            if (j.name().equalsIgnoreCase(t) || j.label.equalsIgnoreCase(t)) {
                return j;
            }
        }
        return null;
    }
}

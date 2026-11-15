package com.klinik.model;

/**
 * Enum jenis surat keterangan medis yang diterbitkan oleh dokter.
 */
public enum JenisSurat {
    IZIN("Surat Izin Sakit"),
    RUJUKAN("Surat Rujukan");

    private final String label;

    JenisSurat(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    public static JenisSurat fromString(String text) {
        if (text == null) return null;
        for (JenisSurat js : values()) {
            if (js.name().equalsIgnoreCase(text.trim()) || js.label.equalsIgnoreCase(text.trim())) {
                return js;
            }
        }
        return null;
    }
}

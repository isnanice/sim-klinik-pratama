package com.klinik.model;

public enum JenisKelamin {
    LAKI_LAKI("Laki-laki"),
    PEREMPUAN("Perempuan");

    private final String label;

    JenisKelamin(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    public static JenisKelamin fromString(String text) {
        if (text == null) return null;
        for (JenisKelamin jk : values()) {
            if (jk.label.equalsIgnoreCase(text.trim()) || jk.name().equalsIgnoreCase(text.trim())) {
                return jk;
            }
        }
        return null;
    }
}


package com.klinik.model;

public enum Role {
    ADMIN("ADMIN"),
    PETUGAS("PETUGAS");

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
}


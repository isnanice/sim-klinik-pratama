package com.klinik.model;

/**
 * Entitas Petugas/Pengguna Klinik Pratama.
 * Mewarisi Orang.
 */
public class Petugas extends Orang {
    private String username;
    private String password;
    private Role role;

    public Petugas() {
        super();
        this.role = Role.PETUGAS;
    }

    public Petugas(String id, String nama, JenisKelamin jenisKelamin, String alamat,
                   String noTelepon, boolean aktif, String username, String password, Role role) {
        super(id, nama, jenisKelamin, alamat, noTelepon, aktif);
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public String getPeran() {
        return role != null ? role.getLabel() : "Petugas";
    }

    @Override
    public String cetakRingkasan() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-18s: %s%n", "ID Petugas", id != null ? id : "-"));
        sb.append(String.format("%-18s: %s%n", "Nama", nama != null ? nama : "-"));
        sb.append(String.format("%-18s: %s%n", "Username", username != null ? username : "-"));
        sb.append(String.format("%-18s: %s%n", "Role", role != null ? role.getLabel() : "-"));
        sb.append(String.format("%-18s: %s%n", "No. Telepon", noTelepon != null ? noTelepon : "-"));
        sb.append(String.format("%-18s: %s%n", "Status", aktif ? "Aktif" : "Nonaktif"));
        return sb.toString();
    }
}


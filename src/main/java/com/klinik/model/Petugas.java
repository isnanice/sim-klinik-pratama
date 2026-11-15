package com.klinik.model;

import com.klinik.exception.DataTidakValidException;

/**
 * Entitas Petugas/Pengguna Klinik Pratama.
 * Mewarisi Orang dan terhubung ke enum Role serta JenisKelamin.
 */
public class Petugas extends Orang implements DapatDicetak {
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
        this.role = role != null ? role : Role.PETUGAS;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) throws DataTidakValidException {
        if (username == null || username.trim().isEmpty()) {
            throw new DataTidakValidException("Username", "Username wajib diisi dan tidak boleh kosong.");
        }
        this.username = username.trim();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) throws DataTidakValidException {
        if (password == null || password.trim().isEmpty()) {
            throw new DataTidakValidException("Password", "Password wajib diisi dan tidak boleh kosong.");
        }
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) throws DataTidakValidException {
        if (role == null) {
            throw new DataTidakValidException("Role", "Role pengguna wajib dipilih.");
        }
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
        sb.append(String.format("%-18s: %s%n", "Jenis Kelamin", jenisKelamin != null ? jenisKelamin.getLabel() : "-"));
        sb.append(String.format("%-18s: %s%n", "Username", username != null ? username : "-"));
        sb.append(String.format("%-18s: %s%n", "Role", role != null ? role.getLabel() : "-"));
        sb.append(String.format("%-18s: %s%n", "No. Telepon", noTelepon != null ? noTelepon : "-"));
        sb.append(String.format("%-18s: %s%n", "Alamat", alamat != null ? alamat : "-"));
        sb.append(String.format("%-18s: %s%n", "Status", aktif ? "Aktif" : "Nonaktif"));
        return sb.toString();
    }

    @Override
    public String toString() {
        return (id != null ? id + " - " : "") + (nama != null ? nama : username) + " (" + getPeran() + ")";
    }
}

package com.klinik.dao;

import com.klinik.exception.DataNotFoundException;
import com.klinik.model.JenisKelamin;
import com.klinik.model.Petugas;
import com.klinik.model.Role;
import com.klinik.util.DatabaseConnection;
import com.klinik.util.PasswordUtil;
import com.klinik.util.PesanDb;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object untuk entitas Petugas/Pengguna Klinik menggunakan PostgreSQL JDBC.
 * Berinteraksi langsung dengan tabel 'petugas' di db_klinik_pratama.
 */
public class PetugasDAO implements GenericDAO<Petugas, String> {

    public String generateNextId() {
        String sql = "SELECT fn_id('PTG', nextval('seq_petugas'))";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (Exception ignored) {}
        return "PTG-" + (System.currentTimeMillis() % 1000000);
    }

    @Override
    public void insert(Petugas p) {
        if (p == null) return;

        String sql = "INSERT INTO petugas (id_petugas, nama, alamat, no_telepon, username, password_hash, role, id_dokter, status_aktif) "
                   + "VALUES (COALESCE(?, fn_id('PTG', nextval('seq_petugas'))), ?, ?, ?, ?, ?, ?, NULL, ?) "
                   + "RETURNING id_petugas";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            String hash = p.getPassword();
            if (hash == null || !hash.contains(":")) {
                hash = PasswordUtil.hash(hash != null ? hash : "klinik123");
            }

            ps.setString(1, (p.getId() != null && !p.getId().isBlank()) ? p.getId() : null);
            ps.setString(2, p.getNama());
            ps.setString(3, p.getAlamat());
            ps.setString(4, p.getNoTelepon());
            ps.setString(5, p.getUsername());
            ps.setString(6, hash);
            ps.setString(7, p.getRole() != null ? p.getRole().name() : "PETUGAS");
            ps.setBoolean(8, p.isAktif());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    p.setId(rs.getString("id_petugas"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Gagal menambah data petugas: " + PesanDb.untukPengguna(e), e);
        }
    }

    public void simpan(Petugas p) {
        insert(p);
    }

    @Override
    public void update(Petugas p) {
        if (p == null || p.getId() == null) return;

        String sql = "UPDATE petugas SET nama = ?, alamat = ?, no_telepon = ?, username = ?, role = ?, status_aktif = ? "
                   + "WHERE id_petugas = ?";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, p.getNama());
            ps.setString(2, p.getAlamat());
            ps.setString(3, p.getNoTelepon());
            ps.setString(4, p.getUsername());
            ps.setString(5, p.getRole() != null ? p.getRole().name() : "PETUGAS");
            ps.setBoolean(6, p.isAktif());
            ps.setString(7, p.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Gagal memperbarui data petugas: " + PesanDb.untukPengguna(e), e);
        }
    }

    public void ubah(Petugas p) {
        update(p);
    }

    @Override
    public void delete(String id) {
        if (id == null) return;
        String sql = "DELETE FROM petugas WHERE id_petugas = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Gagal menghapus data petugas: " + PesanDb.untukPengguna(e), e);
        }
    }

    public void hapus(String id) {
        delete(id);
    }

    public void toggleAktif(String id, boolean aktif) {
        String sql = "UPDATE petugas SET status_aktif = ? WHERE id_petugas = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, aktif);
            ps.setString(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Gagal mengubah status aktif petugas: " + PesanDb.untukPengguna(e), e);
        }
    }

    @Override
    public Optional<Petugas> findById(String id) {
        if (id == null) return Optional.empty();
        String sql = "SELECT * FROM petugas WHERE id_petugas = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPetugas(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal mencari petugas by ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<Petugas> findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) return Optional.empty();
        String sql = "SELECT * FROM petugas WHERE username = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPetugas(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal mencari petugas by username: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Petugas> findAll() {
        List<Petugas> list = new ArrayList<>();
        String sql = "SELECT * FROM petugas ORDER BY id_petugas";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToPetugas(rs));
            }
        } catch (SQLException e) {
            System.err.println("Gagal memuat semua petugas: " + e.getMessage());
        }
        return list;
    }

    public List<Petugas> findActive() {
        List<Petugas> list = new ArrayList<>();
        String sql = "SELECT * FROM petugas WHERE status_aktif = TRUE ORDER BY id_petugas";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToPetugas(rs));
            }
        } catch (SQLException e) {
            System.err.println("Gagal memuat petugas aktif: " + e.getMessage());
        }
        return list;
    }

    private Petugas mapResultSetToPetugas(ResultSet rs) throws SQLException {
        String id = rs.getString("id_petugas");
        String nama = rs.getString("nama");
        String alamat = rs.getString("alamat");
        String noTelp = rs.getString("no_telepon");
        String username = rs.getString("username");
        String pwdHash = rs.getString("password_hash");
        String roleStr = rs.getString("role");
        boolean aktif = rs.getBoolean("status_aktif");

        Role role = Role.PETUGAS;
        try {
            role = Role.valueOf(roleStr);
        } catch (Exception ignored) {}

        return new Petugas(id, nama, JenisKelamin.LAKI_LAKI, alamat, noTelp, aktif, username, pwdHash, role);
    }
}

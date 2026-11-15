package com.klinik.dao;

import com.klinik.model.Layanan;
import com.klinik.model.LayananSpesialis;
import com.klinik.model.LayananUmum;
import com.klinik.util.DatabaseConnection;
import com.klinik.util.PesanDb;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object untuk entitas Layanan menggunakan PostgreSQL JDBC.
 * Berinteraksi langsung dengan tabel 'layanan' di db_klinik_pratama.
 */
public class LayananDAO implements GenericDAO<Layanan, String> {

    public String generateNextId() {
        String sql = "SELECT fn_id('LYN', nextval('seq_layanan'))";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (Exception ignored) {}
        return "LYN-" + (System.currentTimeMillis() % 1000000);
    }

    @Override
    public void insert(Layanan l) {
        if (l == null) return;

        String sql = "INSERT INTO layanan (id_layanan, nama_layanan, jenis_layanan, tarif, keterangan, status_aktif) "
                   + "VALUES (COALESCE(?, fn_id('LYN', nextval('seq_layanan'))), ?, ?, ?, ?, ?) "
                   + "RETURNING id_layanan";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            String jenis = (l instanceof LayananSpesialis) ? "SPESIALIS" : "UMUM";

            ps.setString(1, (l.getId() != null && !l.getId().isBlank()) ? l.getId() : null);
            ps.setString(2, l.getNamaLayanan());
            ps.setString(3, jenis);
            ps.setDouble(4, l.getTarifDasar());
            ps.setString(5, l.getKeterangan());
            ps.setBoolean(6, l.isAktif());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    l.setId(rs.getString("id_layanan"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Gagal menambah data layanan: " + PesanDb.untukPengguna(e), e);
        }
    }

    @Override
    public void update(Layanan l) {
        if (l == null || l.getId() == null) return;

        String sql = "UPDATE layanan SET nama_layanan = ?, jenis_layanan = ?, tarif = ?, keterangan = ?, status_aktif = ? "
                   + "WHERE id_layanan = ?";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            String jenis = (l instanceof LayananSpesialis) ? "SPESIALIS" : "UMUM";

            ps.setString(1, l.getNamaLayanan());
            ps.setString(2, jenis);
            ps.setDouble(3, l.getTarifDasar());
            ps.setString(4, l.getKeterangan());
            ps.setBoolean(5, l.isAktif());
            ps.setString(6, l.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Gagal memperbarui data layanan: " + PesanDb.untukPengguna(e), e);
        }
    }

    @Override
    public void delete(String id) {
        // Aturan bisnis: layanan tidak dihapus permanen, melainkan dinonaktifkan
        toggleAktif(id, false);
    }

    public void toggleAktif(String id, boolean aktif) {
        String sql = "UPDATE layanan SET status_aktif = ? WHERE id_layanan = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, aktif);
            ps.setString(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Gagal mengubah status aktif layanan: " + PesanDb.untukPengguna(e), e);
        }
    }

    @Override
    public Optional<Layanan> findById(String id) {
        if (id == null) return Optional.empty();
        String sql = "SELECT * FROM layanan WHERE id_layanan = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToLayanan(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal mencari layanan by ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Layanan> findAll() {
        List<Layanan> list = new ArrayList<>();
        String sql = "SELECT * FROM layanan ORDER BY id_layanan";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToLayanan(rs));
            }
        } catch (SQLException e) {
            System.err.println("Gagal memuat semua layanan: " + e.getMessage());
        }
        return list;
    }

    public List<Layanan> findActive() {
        List<Layanan> list = new ArrayList<>();
        String sql = "SELECT * FROM layanan WHERE status_aktif = TRUE ORDER BY id_layanan";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToLayanan(rs));
            }
        } catch (SQLException e) {
            System.err.println("Gagal memuat layanan aktif: " + e.getMessage());
        }
        return list;
    }

    public List<Layanan> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        String q = "%" + keyword.trim().toLowerCase() + "%";
        String sql = "SELECT * FROM layanan WHERE LOWER(nama_layanan) LIKE ? OR LOWER(id_layanan) LIKE ? ORDER BY id_layanan";
        List<Layanan> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, q);
            ps.setString(2, q);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToLayanan(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal mencari layanan: " + e.getMessage());
        }
        return list;
    }

    private Layanan mapResultSetToLayanan(ResultSet rs) throws SQLException {
        String id = rs.getString("id_layanan");
        String nama = rs.getString("nama_layanan");
        String jenis = rs.getString("jenis_layanan");
        double tarif = rs.getDouble("tarif");
        String ket = rs.getString("keterangan");
        boolean aktif = rs.getBoolean("status_aktif");

        if ("SPESIALIS".equalsIgnoreCase(jenis)) {
            return new LayananSpesialis(id, nama, tarif, ket, aktif);
        } else {
            return new LayananUmum(id, nama, tarif, ket, aktif);
        }
    }
}

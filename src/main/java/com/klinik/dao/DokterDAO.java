package com.klinik.dao;

import com.klinik.exception.DataMasihDipakaiException;
import com.klinik.model.Dokter;
import com.klinik.model.JenisKelamin;
import com.klinik.util.DatabaseConnection;
import com.klinik.util.PesanDb;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object untuk entitas Dokter menggunakan PostgreSQL JDBC.
 * Berinteraksi langsung dengan tabel 'dokter' di db_klinik_pratama.
 */
public class DokterDAO implements GenericDAO<Dokter, String> {

    public String generateNextId() {
        String sql = "SELECT fn_id('DKT', nextval('seq_dokter'))";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (Exception ignored) {}
        return "DKT-" + (System.currentTimeMillis() % 1000000);
    }

    @Override
    public void insert(Dokter d) {
        if (d == null) return;

        String sql = "INSERT INTO dokter (id_dokter, nama, jenis_kelamin, alamat, no_telepon, nomor_registrasi, bidang_pelayanan, status_aktif) "
                   + "VALUES (COALESCE(?, fn_id('DKT', nextval('seq_dokter'))), ?, ?, ?, ?, ?, ?, ?) "
                   + "RETURNING id_dokter";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, (d.getId() != null && !d.getId().isBlank()) ? d.getId() : null);
            ps.setString(2, d.getNama());
            ps.setString(3, d.getJenisKelamin() != null ? d.getJenisKelamin().getLabel() : "Laki-laki");
            ps.setString(4, d.getAlamat());
            ps.setString(5, d.getNoTelepon());
            ps.setString(6, d.getNomorRegistrasi());
            ps.setString(7, d.getBidangPelayanan());
            ps.setBoolean(8, d.isAktif());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    d.setId(rs.getString("id_dokter"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Gagal menambah data dokter: " + PesanDb.untukPengguna(e), e);
        }
    }

    @Override
    public void update(Dokter d) {
        if (d == null || d.getId() == null) return;

        String sql = "UPDATE dokter SET nama = ?, jenis_kelamin = ?, alamat = ?, no_telepon = ?, "
                   + "nomor_registrasi = ?, bidang_pelayanan = ?, status_aktif = ? WHERE id_dokter = ?";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, d.getNama());
            ps.setString(2, d.getJenisKelamin() != null ? d.getJenisKelamin().getLabel() : "Laki-laki");
            ps.setString(3, d.getAlamat());
            ps.setString(4, d.getNoTelepon());
            ps.setString(5, d.getNomorRegistrasi());
            ps.setString(6, d.getBidangPelayanan());
            ps.setBoolean(7, d.isAktif());
            ps.setString(8, d.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Gagal memperbarui data dokter: " + PesanDb.untukPengguna(e), e);
        }
    }

    @Override
    public void delete(String id) throws DataMasihDipakaiException {
        if (id == null) return;

        String sql = "DELETE FROM dokter WHERE id_dokter = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (PesanDb.dataDipakai(e)) {
                throw new DataMasihDipakaiException("Dokter", id, PesanDb.untukPengguna(e));
            }
            throw new RuntimeException("Gagal menghapus data dokter: " + PesanDb.untukPengguna(e), e);
        }
    }

    public void toggleAktif(String id, boolean aktif) {
        String sql = "UPDATE dokter SET status_aktif = ? WHERE id_dokter = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, aktif);
            ps.setString(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Gagal mengubah status aktif dokter: " + PesanDb.untukPengguna(e), e);
        }
    }

    @Override
    public Optional<Dokter> findById(String id) {
        if (id == null) return Optional.empty();
        String sql = "SELECT * FROM dokter WHERE id_dokter = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToDokter(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal mencari dokter by ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Dokter> findAll() {
        List<Dokter> list = new ArrayList<>();
        String sql = "SELECT * FROM dokter ORDER BY id_dokter";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToDokter(rs));
            }
        } catch (SQLException e) {
            System.err.println("Gagal memuat semua dokter: " + e.getMessage());
        }
        return list;
    }

    public List<Dokter> findActive() {
        List<Dokter> list = new ArrayList<>();
        String sql = "SELECT * FROM dokter WHERE status_aktif = TRUE ORDER BY id_dokter";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToDokter(rs));
            }
        } catch (SQLException e) {
            System.err.println("Gagal memuat dokter aktif: " + e.getMessage());
        }
        return list;
    }

    public List<Dokter> search(String keyword, String criteria) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        String q = "%" + keyword.trim().toLowerCase() + "%";
        String sql;
        if ("Nama".equalsIgnoreCase(criteria)) {
            sql = "SELECT * FROM dokter WHERE LOWER(nama) LIKE ? ORDER BY id_dokter";
        } else if ("Bidang Pelayanan".equalsIgnoreCase(criteria)) {
            sql = "SELECT * FROM dokter WHERE LOWER(bidang_pelayanan) LIKE ? ORDER BY id_dokter";
        } else {
            sql = "SELECT * FROM dokter WHERE LOWER(nama) LIKE ? OR LOWER(bidang_pelayanan) LIKE ? OR LOWER(id_dokter) LIKE ? ORDER BY id_dokter";
        }

        List<Dokter> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, q);
            if (!"Nama".equalsIgnoreCase(criteria) && !"Bidang Pelayanan".equalsIgnoreCase(criteria)) {
                ps.setString(2, q);
                ps.setString(3, q);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToDokter(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal mencari dokter: " + e.getMessage());
        }
        return list;
    }

    private Dokter mapResultSetToDokter(ResultSet rs) throws SQLException {
        String id = rs.getString("id_dokter");
        String nama = rs.getString("nama");
        String jkStr = rs.getString("jenis_kelamin");
        JenisKelamin jk = JenisKelamin.fromString(jkStr);
        String alamat = rs.getString("alamat");
        String noTelp = rs.getString("no_telepon");
        String noReg = rs.getString("nomor_registrasi");
        String bidang = rs.getString("bidang_pelayanan");
        boolean aktif = rs.getBoolean("status_aktif");

        return new Dokter(id, nama, jk, alamat, noTelp, noReg, bidang, aktif);
    }
}

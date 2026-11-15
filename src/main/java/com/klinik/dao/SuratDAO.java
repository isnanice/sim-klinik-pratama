package com.klinik.dao;

import com.klinik.model.JenisSurat;
import com.klinik.model.StatusSurat;
import com.klinik.model.Surat;
import com.klinik.util.DatabaseConnection;
import com.klinik.util.PesanDb;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object untuk entitas Surat menggunakan PostgreSQL JDBC.
 * Berinteraksi langsung dengan tabel 'surat' di db_klinik_pratama.
 */
public class SuratDAO implements GenericDAO<Surat, String> {

    public String generateNextId() {
        String sql = "SELECT fn_id('SRT', nextval('seq_surat'))";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (Exception ignored) {}
        return "SRT-" + (System.currentTimeMillis() % 1000000);
    }

    @Override
    public void insert(Surat s) {
        if (s == null) return;

        String sql = "INSERT INTO surat (id_surat, id_pemeriksaan, jenis, isi, jumlah_hari, tujuan_rujukan, tanggal_dibuat, status, diserahkan_oleh) "
                   + "VALUES (COALESCE(?, fn_id('SRT', nextval('seq_surat'))), ?, ?, ?, ?, ?, COALESCE(?, CURRENT_DATE), COALESCE(?, 'DITERUSKAN'), ?) "
                   + "RETURNING id_surat, tanggal_dibuat";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, (s.getIdSurat() != null && !s.getIdSurat().isBlank()) ? s.getIdSurat() : null);
            ps.setString(2, s.getIdPemeriksaan());
            ps.setString(3, s.getJenis() != null ? s.getJenis().name() : "IZIN");
            ps.setString(4, s.getIsi() != null ? s.getIsi() : "Keterangan surat");

            if (s.getJumlahHari() > 0) {
                ps.setInt(5, s.getJumlahHari());
            } else {
                ps.setNull(5, Types.INTEGER);
            }

            ps.setString(6, (s.getTujuanRujukan() != null && !s.getTujuanRujukan().isBlank()) ? s.getTujuanRujukan() : null);
            ps.setDate(7, s.getTanggalDibuat() != null ? Date.valueOf(s.getTanggalDibuat()) : Date.valueOf(LocalDate.now()));
            ps.setString(8, s.getStatus() != null ? s.getStatus().name() : "DITERUSKAN");
            ps.setString(9, (s.getDiserahkanOleh() != null && !s.getDiserahkanOleh().isBlank()) ? s.getDiserahkanOleh() : null);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    s.setIdSurat(rs.getString("id_surat"));
                    Date tgl = rs.getDate("tanggal_dibuat");
                    if (tgl != null) s.setTanggalDibuat(tgl.toLocalDate());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Gagal menambah surat: " + PesanDb.untukPengguna(e), e);
        }
    }

    public void simpan(Surat s) {
        insert(s);
    }

    @Override
    public void update(Surat s) {
        if (s == null || s.getIdSurat() == null) return;

        String sql = "UPDATE surat SET isi = ?, status = ?, diserahkan_oleh = ? WHERE id_surat = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, s.getIsi());
            ps.setString(2, s.getStatus() != null ? s.getStatus().name() : "DITERUSKAN");
            ps.setString(3, s.getDiserahkanOleh());
            ps.setString(4, s.getIdSurat());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Gagal memperbarui surat: " + PesanDb.untukPengguna(e), e);
        }
    }

    public void ubah(Surat s) {
        update(s);
    }

    @Override
    public void delete(String id) {
        if (id == null) return;
        String sql = "DELETE FROM surat WHERE id_surat = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Gagal menghapus surat: " + PesanDb.untukPengguna(e), e);
        }
    }

    public void hapus(String id) {
        delete(id);
    }

    @Override
    public Optional<Surat> findById(String id) {
        if (id == null) return Optional.empty();
        String sql = "SELECT * FROM surat WHERE id_surat = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSurat(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal mencari surat by ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<Surat> findByIdPemeriksaan(String idPemeriksaan) {
        List<Surat> list = new ArrayList<>();
        String sql = "SELECT * FROM surat WHERE id_pemeriksaan = ? ORDER BY id_surat";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, idPemeriksaan);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSurat(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal memuat surat by pemeriksaan: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<Surat> findAll() {
        List<Surat> list = new ArrayList<>();
        String sql = "SELECT * FROM surat ORDER BY id_surat DESC";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToSurat(rs));
            }
        } catch (SQLException e) {
            System.err.println("Gagal memuat semua surat: " + e.getMessage());
        }
        return list;
    }

    private Surat mapResultSetToSurat(ResultSet rs) throws SQLException {
        String id = rs.getString("id_surat");
        String idPemeriksaan = rs.getString("id_pemeriksaan");
        String jenisStr = rs.getString("jenis");
        String isi = rs.getString("isi");
        int jmlHari = rs.getInt("jumlah_hari");
        String tujuan = rs.getString("tujuan_rujukan");
        Date tgl = rs.getDate("tanggal_dibuat");
        String statusStr = rs.getString("status");
        String diserahkanOleh = rs.getString("diserahkan_oleh");

        JenisSurat jenis = JenisSurat.fromString(jenisStr);
        StatusSurat status = StatusSurat.fromString(statusStr);
        LocalDate tglDibuat = tgl != null ? tgl.toLocalDate() : LocalDate.now();

        try {
            return new Surat(id, idPemeriksaan, jenis, isi, jmlHari, tujuan, tglDibuat, status, diserahkanOleh);
        } catch (Exception e) {
            Surat s = new Surat();
            s.setIdSurat(id);
            s.setIdPemeriksaan(idPemeriksaan);
            return s;
        }
    }
}

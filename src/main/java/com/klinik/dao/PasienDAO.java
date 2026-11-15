package com.klinik.dao;

import com.klinik.exception.DataMasihDipakaiException;
import com.klinik.exception.DataNotFoundException;
import com.klinik.model.JenisKelamin;
import com.klinik.model.Pasien;
import com.klinik.util.DatabaseConnection;
import com.klinik.util.PesanDb;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object untuk entitas Pasien menggunakan PostgreSQL JDBC.
 * Berinteraksi langsung dengan tabel 'pasien' di db_klinik_pratama.
 */
public class PasienDAO implements GenericDAO<Pasien, String> {
    private PemeriksaanDAO pemeriksaanDAO;

    public PasienDAO() {
    }

    public PasienDAO(PemeriksaanDAO pemeriksaanDAO) {
        this.pemeriksaanDAO = pemeriksaanDAO;
    }

    public void setPemeriksaanDAO(PemeriksaanDAO pemeriksaanDAO) {
        this.pemeriksaanDAO = pemeriksaanDAO;
    }

    public String generateNextId() {
        String sql = "SELECT fn_id('PSN', nextval('seq_pasien'))";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (Exception ignored) {}
        return "PSN-" + (System.currentTimeMillis() % 1000000);
    }

    @Override
    public void insert(Pasien p) {
        if (p == null) return;

        String sql = "INSERT INTO pasien (id_pasien, nik, no_bpjs, nama, jenis_kelamin, tanggal_lahir, alamat, no_telepon, tanggal_registrasi, status_aktif) "
                   + "VALUES (COALESCE(?, fn_id('PSN', nextval('seq_pasien'))), ?, ?, ?, ?, ?, ?, ?, COALESCE(?, CURRENT_DATE), ?) "
                   + "RETURNING id_pasien, tanggal_registrasi";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            String nik = p.getNik();
            if (nik == null || nik.trim().isEmpty()) {
                nik = String.format("327301%010d", Math.abs(System.nanoTime() % 10000000000L));
                p.setNik(nik);
            }

            ps.setString(1, (p.getId() != null && !p.getId().isBlank()) ? p.getId() : null);
            ps.setString(2, nik);
            ps.setString(3, (p.getNoBpjs() != null && !p.getNoBpjs().isBlank()) ? p.getNoBpjs().trim() : null);
            ps.setString(4, p.getNama());
            ps.setString(5, p.getJenisKelamin() != null ? p.getJenisKelamin().getLabel() : "Laki-laki");
            ps.setDate(6, p.getTanggalLahir() != null ? Date.valueOf(p.getTanggalLahir()) : Date.valueOf(LocalDate.of(2000, 1, 1)));
            ps.setString(7, p.getAlamat());
            ps.setString(8, p.getNoTelepon());
            ps.setDate(9, p.getTanggalRegistrasi() != null ? Date.valueOf(p.getTanggalRegistrasi()) : Date.valueOf(LocalDate.now()));
            ps.setBoolean(10, p.isAktif());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    p.setId(rs.getString("id_pasien"));
                    Date tglReg = rs.getDate("tanggal_registrasi");
                    if (tglReg != null) {
                        p.setTanggalRegistrasi(tglReg.toLocalDate());
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Gagal menambah data pasien: " + PesanDb.untukPengguna(e), e);
        }
    }

    public void simpan(Pasien p) {
        insert(p);
    }

    @Override
    public void update(Pasien p) {
        if (p == null || p.getId() == null) return;

        String sql = "UPDATE pasien SET nik = ?, no_bpjs = ?, nama = ?, jenis_kelamin = ?, tanggal_lahir = ?, "
                   + "alamat = ?, no_telepon = ?, status_aktif = ? WHERE id_pasien = ?";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            String nik = p.getNik();
            if (nik == null || nik.trim().isEmpty()) {
                nik = String.format("327301%010d", Math.abs(System.nanoTime() % 10000000000L));
                p.setNik(nik);
            }

            ps.setString(1, nik);
            ps.setString(2, (p.getNoBpjs() != null && !p.getNoBpjs().isBlank()) ? p.getNoBpjs().trim() : null);
            ps.setString(3, p.getNama());
            ps.setString(4, p.getJenisKelamin() != null ? p.getJenisKelamin().getLabel() : "Laki-laki");
            ps.setDate(5, p.getTanggalLahir() != null ? Date.valueOf(p.getTanggalLahir()) : Date.valueOf(LocalDate.of(2000, 1, 1)));
            ps.setString(6, p.getAlamat());
            ps.setString(7, p.getNoTelepon());
            ps.setBoolean(8, p.isAktif());
            ps.setString(9, p.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Gagal memperbarui data pasien: " + PesanDb.untukPengguna(e), e);
        }
    }

    public void ubah(Pasien p) {
        update(p);
    }

    @Override
    public void delete(String id) throws DataMasihDipakaiException {
        if (id == null) return;

        if (hasRiwayatPemeriksaan(id)) {
            Optional<Pasien> p = findById(id);
            String nama = p.map(Pasien::getNama).orElse(id);
            throw new DataMasihDipakaiException("Pasien", id,
                    "Pasien " + nama + " (" + id + ") memiliki riwayat pemeriksaan dan tidak dapat dihapus permanen. Gunakan fitur Nonaktifkan.");
        }

        String sql = "DELETE FROM pasien WHERE id_pasien = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (PesanDb.dataDipakai(e)) {
                throw new DataMasihDipakaiException("Pasien", id, PesanDb.untukPengguna(e));
            }
            throw new RuntimeException("Gagal menghapus data pasien: " + PesanDb.untukPengguna(e), e);
        }
    }

    public void hapus(String id) throws DataMasihDipakaiException {
        delete(id);
    }

    public boolean hasRiwayatPemeriksaan(String pasienId) {
        if (pasienId == null || pasienId.trim().isEmpty()) return false;
        if (pemeriksaanDAO != null && pemeriksaanDAO.existsByPasienId(pasienId)) {
            return true;
        }
        String sql = "SELECT 1 FROM pemeriksaan WHERE id_pasien = ? LIMIT 1";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, pasienId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ignored) {
            return false;
        }
    }

    public void toggleAktif(String id, boolean aktif) {
        String sql = "UPDATE pasien SET status_aktif = ? WHERE id_pasien = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, aktif);
            ps.setString(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Gagal mengubah status aktif pasien: " + PesanDb.untukPengguna(e), e);
        }
    }

    public void nonaktifkanPasien(String id) throws DataNotFoundException {
        if (findById(id).isEmpty()) {
            throw new DataNotFoundException("Pasien dengan ID " + id + " tidak ditemukan.");
        }
        toggleAktif(id, false);
    }

    @Override
    public Optional<Pasien> findById(String id) {
        if (id == null) return Optional.empty();
        String sql = "SELECT * FROM pasien WHERE id_pasien = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPasien(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal mencari pasien by ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<Pasien> findByNik(String nik) {
        if (nik == null || nik.trim().isEmpty()) return Optional.empty();
        String sql = "SELECT * FROM pasien WHERE nik = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nik.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPasien(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal mencari pasien by NIK: " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<Pasien> findByNama(String nama) {
        if (nama == null || nama.trim().isEmpty()) return findAll();
        String sql = "SELECT * FROM pasien WHERE LOWER(nama) LIKE ? ORDER BY id_pasien";
        List<Pasien> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + nama.trim().toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPasien(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal mencari pasien by Nama: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<Pasien> findAll() {
        List<Pasien> list = new ArrayList<>();
        String sql = "SELECT * FROM pasien ORDER BY id_pasien";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToPasien(rs));
            }
        } catch (SQLException e) {
            System.err.println("Gagal memuat semua pasien: " + e.getMessage());
        }
        return list;
    }

    public List<Pasien> findActive() {
        List<Pasien> list = new ArrayList<>();
        String sql = "SELECT * FROM pasien WHERE status_aktif = TRUE ORDER BY id_pasien";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToPasien(rs));
            }
        } catch (SQLException e) {
            System.err.println("Gagal memuat pasien aktif: " + e.getMessage());
        }
        return list;
    }

    public List<Pasien> search(String keyword, String criteria) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        String q = "%" + keyword.trim().toLowerCase() + "%";
        String sql;
        String krit = criteria == null ? "Semua" : criteria.trim();

        if ("ID".equalsIgnoreCase(krit)) {
            sql = "SELECT * FROM pasien WHERE LOWER(id_pasien) LIKE ? ORDER BY id_pasien";
        } else if ("NIK".equalsIgnoreCase(krit)) {
            sql = "SELECT * FROM pasien WHERE LOWER(nik) LIKE ? ORDER BY id_pasien";
        } else if ("Nama".equalsIgnoreCase(krit)) {
            sql = "SELECT * FROM pasien WHERE LOWER(nama) LIKE ? ORDER BY id_pasien";
        } else if ("No. BPJS".equalsIgnoreCase(krit) || "BPJS".equalsIgnoreCase(krit)) {
            sql = "SELECT * FROM pasien WHERE LOWER(no_bpjs) LIKE ? ORDER BY id_pasien";
        } else {
            sql = "SELECT * FROM pasien WHERE LOWER(id_pasien) LIKE ? OR LOWER(nik) LIKE ? OR LOWER(nama) LIKE ? OR LOWER(no_bpjs) LIKE ? ORDER BY id_pasien";
        }

        List<Pasien> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, q);
            if ("Semua".equalsIgnoreCase(krit)) {
                ps.setString(2, q);
                ps.setString(3, q);
                ps.setString(4, q);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPasien(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal mencari pasien: " + e.getMessage());
        }
        return list;
    }

    public List<Pasien> cari(String keyword) {
        return search(keyword, "Semua");
    }

    private Pasien mapResultSetToPasien(ResultSet rs) throws SQLException {
        String id = rs.getString("id_pasien");
        String nik = rs.getString("nik");
        String noBpjs = rs.getString("no_bpjs");
        String nama = rs.getString("nama");
        String jkStr = rs.getString("jenis_kelamin");
        JenisKelamin jk = JenisKelamin.fromString(jkStr);
        Date tglLahir = rs.getDate("tanggal_lahir");
        String alamat = rs.getString("alamat");
        String noTelp = rs.getString("no_telepon");
        Date tglReg = rs.getDate("tanggal_registrasi");
        boolean aktif = rs.getBoolean("status_aktif");

        LocalDate localLahir = tglLahir != null ? tglLahir.toLocalDate() : null;
        LocalDate localReg = tglReg != null ? tglReg.toLocalDate() : LocalDate.now();

        return new Pasien(id, nik, nama, jk, localLahir, alamat, noTelp, noBpjs, localReg, aktif);
    }
}

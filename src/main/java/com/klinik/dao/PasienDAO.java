package com.klinik.dao;

import com.klinik.exception.DataMasihDipakaiException;
import com.klinik.exception.DataNotFoundException;
import com.klinik.model.JenisKelamin;
import com.klinik.model.Pasien;
import com.klinik.util.DatabaseConnection;
import com.klinik.util.Jdbc;
import com.klinik.util.PesanDb;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object untuk entitas Pasien menggunakan JDBC PostgreSQL (tabel pasien).
 * Mengimplementasikan GenericDAO serta mendukung pencarian NIK/ID/Nama/BPJS, nonaktifkan pasien,
 * dan proteksi hapus permanen bila pasien memiliki riwayat pemeriksaan (ON DELETE RESTRICT / SQLState 23503).
 */
public class PasienDAO implements GenericDAO<Pasien, String> {
    private PemeriksaanDAO pemeriksaanDAO;

    public PasienDAO() {
    }

    public PasienDAO(String ignoredDataDir) {
    }

    public PasienDAO(PemeriksaanDAO pemeriksaanDAO) {
        this.pemeriksaanDAO = pemeriksaanDAO;
    }

    public PasienDAO(PemeriksaanDAO pemeriksaanDAO, String ignoredDataDir) {
        this.pemeriksaanDAO = pemeriksaanDAO;
    }

    public void setPemeriksaanDAO(PemeriksaanDAO pemeriksaanDAO) {
        this.pemeriksaanDAO = pemeriksaanDAO;
    }

    public String generateNextId() {
        try {
            return Jdbc.nextId("PSN", "seq_pasien");
        } catch (SQLException e) {
            throw Jdbc.toKlinikException(e, "Pasien", null);
        }
    }

    @Override
    public void insert(Pasien pasien) {
        if (pasien == null) {
            return;
        }
        boolean hasId = pasien.getId() != null && !pasien.getId().trim().isEmpty();
        String sql = hasId
                ? "INSERT INTO pasien (id_pasien, nik, no_bpjs, nama, jenis_kelamin, tanggal_lahir, alamat, no_telepon, tanggal_registrasi, status_aktif) "
                  + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id_pasien"
                : "INSERT INTO pasien (nik, no_bpjs, nama, jenis_kelamin, tanggal_lahir, alamat, no_telepon, tanggal_registrasi, status_aktif) "
                  + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id_pasien";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            if (hasId) {
                ps.setString(idx++, pasien.getId().trim());
            }
            ps.setString(idx++, pasien.getNik());
            ps.setString(idx++, Jdbc.emptyToNull(pasien.getNoBpjs()));
            ps.setString(idx++, pasien.getNama());
            ps.setString(idx++, pasien.getJenisKelamin() != null ? pasien.getJenisKelamin().getLabel() : null);
            ps.setDate(idx++, pasien.getTanggalLahir() != null ? Date.valueOf(pasien.getTanggalLahir()) : null);
            ps.setString(idx++, Jdbc.emptyToNull(pasien.getAlamat()));
            ps.setString(idx++, pasien.getNoTelepon());
            LocalDate tglReg = pasien.getTanggalRegistrasi() != null ? pasien.getTanggalRegistrasi() : LocalDate.now();
            ps.setDate(idx++, Date.valueOf(tglReg));
            ps.setBoolean(idx, pasien.isAktif());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    pasien.setId(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            throw Jdbc.toKlinikException(e, "Pasien", pasien.getId());
        }
    }

    public void simpan(Pasien pasien) {
        insert(pasien);
    }

    @Override
    public void update(Pasien pasien) {
        if (pasien == null || pasien.getId() == null) {
            return;
        }
        String sql = "UPDATE pasien SET nik = ?, no_bpjs = ?, nama = ?, jenis_kelamin = ?, tanggal_lahir = ?, "
                   + "alamat = ?, no_telepon = ?, tanggal_registrasi = ?, status_aktif = ? WHERE id_pasien = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pasien.getNik());
            ps.setString(2, Jdbc.emptyToNull(pasien.getNoBpjs()));
            ps.setString(3, pasien.getNama());
            ps.setString(4, pasien.getJenisKelamin() != null ? pasien.getJenisKelamin().getLabel() : null);
            ps.setDate(5, pasien.getTanggalLahir() != null ? Date.valueOf(pasien.getTanggalLahir()) : null);
            ps.setString(6, Jdbc.emptyToNull(pasien.getAlamat()));
            ps.setString(7, pasien.getNoTelepon());
            LocalDate tglReg = pasien.getTanggalRegistrasi() != null ? pasien.getTanggalRegistrasi() : LocalDate.now();
            ps.setDate(8, Date.valueOf(tglReg));
            ps.setBoolean(9, pasien.isAktif());
            ps.setString(10, pasien.getId().trim());

            if (ps.executeUpdate() == 0) {
                throw new DataNotFoundException("Pasien dengan ID " + pasien.getId() + " tidak ditemukan.");
            }
        } catch (SQLException e) {
            throw Jdbc.toKlinikException(e, "Pasien", pasien.getId());
        }
    }

    public void ubah(Pasien pasien) {
        update(pasien);
    }

    /**
     * BR-11: Menghapus pasien secara permanen.
     * Menangkap pelanggaran ON DELETE RESTRICT (SQLState 23503) dari PostgreSQL dan memetakannya ke DataMasihDipakaiException.
     */
    @Override
    public void delete(String id) throws DataMasihDipakaiException {
        if (id == null || id.trim().isEmpty()) {
            return;
        }
        String cleanId = id.trim();
        Optional<Pasien> existing = findById(cleanId);
        if (pemeriksaanDAO != null && pemeriksaanDAO.existsByPasienId(cleanId)) {
            String nama = existing.map(Pasien::getNama).orElse(cleanId);
            throw new DataMasihDipakaiException("Pasien", cleanId,
                    "Pasien " + nama + " (" + cleanId + ") memiliki riwayat pemeriksaan dan tidak dapat dihapus permanen. Gunakan fitur Nonaktifkan.");
        }

        String sql = "DELETE FROM pasien WHERE id_pasien = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cleanId);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (PesanDb.dataDipakai(e)) {
                String nama = existing.map(Pasien::getNama).orElse(cleanId);
                throw new DataMasihDipakaiException("Pasien", cleanId,
                        "Pasien " + nama + " (" + cleanId + ") memiliki riwayat pemeriksaan dan tidak dapat dihapus permanen. Gunakan fitur Nonaktifkan.");
            }
            throw Jdbc.toKlinikException(e, "Pasien", cleanId);
        }
    }

    public void hapus(String id) throws DataMasihDipakaiException {
        delete(id);
    }

    /**
     * Mengecek apakah pasien dengan ID tertentu memiliki riwayat pemeriksaan di PostgreSQL.
     */
    public boolean hasRiwayatPemeriksaan(String pasienId) {
        if (pasienId == null || pasienId.trim().isEmpty()) {
            return false;
        }
        if (pemeriksaanDAO != null && pemeriksaanDAO.existsByPasienId(pasienId.trim())) {
            return true;
        }
        String sql = "SELECT 1 FROM pemeriksaan WHERE id_pasien = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pasienId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Mengubah status aktif/nonaktif pasien di PostgreSQL.
     */
    public void toggleAktif(String id, boolean aktif) {
        if (id == null || id.trim().isEmpty()) {
            return;
        }
        String sql = "UPDATE pasien SET status_aktif = ? WHERE id_pasien = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, aktif);
            ps.setString(2, id.trim());
            if (ps.executeUpdate() == 0) {
                throw new DataNotFoundException("Pasien dengan ID " + id + " tidak ditemukan.");
            }
        } catch (SQLException e) {
            throw Jdbc.toKlinikException(e, "Pasien", id);
        }
    }

    /**
     * Menonaktifkan pasien (soft-delete) berdasarkan ID.
     */
    public void nonaktifkanPasien(String id) throws DataNotFoundException {
        toggleAktif(id, false);
    }

    @Override
    public Optional<Pasien> findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return Optional.empty();
        }
        String sql = "SELECT id_pasien, nik, no_bpjs, nama, jenis_kelamin, tanggal_lahir, alamat, no_telepon, tanggal_registrasi, status_aktif "
                   + "FROM pasien WHERE id_pasien = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw Jdbc.toKlinikException(e, "Pasien", id);
        }
        return Optional.empty();
    }

    public Optional<Pasien> findByNik(String nik) {
        if (nik == null || nik.trim().isEmpty()) {
            return Optional.empty();
        }
        String sql = "SELECT id_pasien, nik, no_bpjs, nama, jenis_kelamin, tanggal_lahir, alamat, no_telepon, tanggal_registrasi, status_aktif "
                   + "FROM pasien WHERE nik = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nik.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw Jdbc.toKlinikException(e, "Pasien", nik);
        }
        return Optional.empty();
    }

    public List<Pasien> findByNama(String nama) {
        return search(nama, "Nama");
    }

    @Override
    public List<Pasien> findAll() {
        List<Pasien> list = new ArrayList<>();
        String sql = "SELECT id_pasien, nik, no_bpjs, nama, jenis_kelamin, tanggal_lahir, alamat, no_telepon, tanggal_registrasi, status_aktif "
                   + "FROM pasien ORDER BY id_pasien";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw Jdbc.toKlinikException(e, "Pasien", null);
        }
        return list;
    }

    public List<Pasien> findActive() {
        List<Pasien> list = new ArrayList<>();
        String sql = "SELECT id_pasien, nik, no_bpjs, nama, jenis_kelamin, tanggal_lahir, alamat, no_telepon, tanggal_registrasi, status_aktif "
                   + "FROM pasien WHERE status_aktif = TRUE ORDER BY id_pasien";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw Jdbc.toKlinikException(e, "Pasien", null);
        }
        return list;
    }

    /**
     * Pencarian pasien berdasarkan kata kunci dan kriteria ("Semua", "ID", "NIK", "Nama", "No. BPJS").
     */
    public List<Pasien> search(String keyword, String criteria) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        String kw = "%" + keyword.trim().toLowerCase() + "%";
        String krit = criteria == null ? "Semua" : criteria.trim();
        String whereClause;
        boolean multiParam = false;

        if ("ID".equalsIgnoreCase(krit)) {
            whereClause = "lower(id_pasien) LIKE ?";
        } else if ("NIK".equalsIgnoreCase(krit)) {
            whereClause = "lower(nik) LIKE ?";
        } else if ("Nama".equalsIgnoreCase(krit)) {
            whereClause = "lower(nama) LIKE ?";
        } else if ("No. BPJS".equalsIgnoreCase(krit) || "BPJS".equalsIgnoreCase(krit)) {
            whereClause = "lower(COALESCE(no_bpjs, '')) LIKE ?";
        } else {
            whereClause = "(lower(id_pasien) LIKE ? OR lower(nik) LIKE ? OR lower(nama) LIKE ? OR lower(COALESCE(no_bpjs, '')) LIKE ?)";
            multiParam = true;
        }

        List<Pasien> list = new ArrayList<>();
        String sql = "SELECT id_pasien, nik, no_bpjs, nama, jenis_kelamin, tanggal_lahir, alamat, no_telepon, tanggal_registrasi, status_aktif "
                   + "FROM pasien WHERE " + whereClause + " ORDER BY id_pasien";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kw);
            if (multiParam) {
                ps.setString(2, kw);
                ps.setString(3, kw);
                ps.setString(4, kw);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw Jdbc.toKlinikException(e, "Pasien", null);
        }
        return list;
    }

    public List<Pasien> cari(String keyword) {
        return search(keyword, "Semua");
    }

    private Pasien mapRow(ResultSet rs) throws SQLException {
        Date tglLahir = rs.getDate("tanggal_lahir");
        Date tglReg = rs.getDate("tanggal_registrasi");
        return new Pasien(
                rs.getString("id_pasien"),
                rs.getString("nik"),
                rs.getString("nama"),
                JenisKelamin.fromString(rs.getString("jenis_kelamin")),
                tglLahir != null ? tglLahir.toLocalDate() : null,
                rs.getString("alamat"),
                rs.getString("no_telepon"),
                rs.getString("no_bpjs"),
                tglReg != null ? tglReg.toLocalDate() : LocalDate.now(),
                rs.getBoolean("status_aktif")
        );
    }
}

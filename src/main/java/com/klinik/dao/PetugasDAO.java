package com.klinik.dao;

import com.klinik.exception.DataMasihDipakaiException;
import com.klinik.exception.DataNotFoundException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.model.Petugas;
import com.klinik.model.Role;
import com.klinik.util.DatabaseConnection;
import com.klinik.util.PasswordUtil;
import com.klinik.util.PesanDb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object untuk entitas Petugas/Pengguna Klinik menggunakan JDBC PostgreSQL (tabel petugas).
 * Menyediakan pencarian berdasarkan username, validasi status_aktif (BR-01), dan proteksi hapus (BR-11).
 */
public class PetugasDAO implements GenericDAO<Petugas, String> {

    public PetugasDAO() {
    }

    public PetugasDAO(String ignoredDataDir) {
    }

    public String generateNextId() {
        return Jdbc.idBerikut("PTG", "seq_petugas");
    }

    @Override
    public void insert(Petugas entity) {
        if (entity == null) {
            return;
        }
        ensurePasswordHashed(entity);
        boolean hasId = entity.getId() != null && !entity.getId().trim().isEmpty();
        String sql = hasId
                ? "INSERT INTO petugas (id_petugas, nama, alamat, no_telepon, username, password_hash, role, id_dokter, status_aktif) "
                  + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id_petugas"
                : "INSERT INTO petugas (nama, alamat, no_telepon, username, password_hash, role, id_dokter, status_aktif) "
                  + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id_petugas";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            if (hasId) {
                ps.setString(idx++, entity.getId().trim());
            }
            ps.setString(idx++, entity.getNama());
            ps.setString(idx++, Jdbc.emptyToNull(entity.getAlamat()));
            ps.setString(idx++, entity.getNoTelepon());
            ps.setString(idx++, entity.getUsername());
            ps.setString(idx++, entity.getPasswordHash());
            Role role = entity.getRole() != null ? entity.getRole() : Role.PETUGAS;
            ps.setString(idx++, role.name());
            ps.setString(idx++, role == Role.DOKTER ? Jdbc.emptyToNull(entity.getIdDokter()) : null);
            ps.setBoolean(idx, entity.isAktif());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    entity.setId(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            throw Jdbc.toKlinikException(e, "Petugas", entity.getId());
        }
    }

    @Override
    public void update(Petugas entity) {
        if (entity == null || entity.getId() == null) {
            return;
        }
        ensurePasswordHashed(entity);
        String sql = "UPDATE petugas SET nama = ?, alamat = ?, no_telepon = ?, username = ?, password_hash = ?, "
                   + "role = ?, id_dokter = ?, status_aktif = ? WHERE id_petugas = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            Role role = entity.getRole() != null ? entity.getRole() : Role.PETUGAS;
            ps.setString(1, entity.getNama());
            ps.setString(2, Jdbc.emptyToNull(entity.getAlamat()));
            ps.setString(3, entity.getNoTelepon());
            ps.setString(4, entity.getUsername());
            ps.setString(5, entity.getPasswordHash());
            ps.setString(6, role.name());
            ps.setString(7, role == Role.DOKTER ? Jdbc.emptyToNull(entity.getIdDokter()) : null);
            ps.setBoolean(8, entity.isAktif());
            ps.setString(9, entity.getId().trim());
            if (ps.executeUpdate() == 0) {
                throw new DataNotFoundException("Petugas dengan ID " + entity.getId() + " tidak ditemukan.");
            }
        } catch (SQLException e) {
            throw Jdbc.toKlinikException(e, "Petugas", entity.getId());
        }
    }

    /**
     * BR-11: Menangkap pelanggaran ON DELETE RESTRICT (SQLState 23503 / 23001) dan memetakannya ke DataMasihDipakaiException.
     */
    @Override
    public void delete(String id) throws DataMasihDipakaiException {
        if (id == null || id.trim().isEmpty()) {
            return;
        }
        Optional<Petugas> existing = findById(id.trim());
        String sql = "DELETE FROM petugas WHERE id_petugas = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.trim());
            ps.executeUpdate();
        } catch (SQLException e) {
            if (PesanDb.dataDipakai(e)) {
                String label = existing.map(Petugas::getNama).orElse(id.trim());
                throw new DataMasihDipakaiException("Petugas", id.trim(),
                        "Petugas " + label + " (" + id.trim() + ") memiliki riwayat pemeriksaan dan tidak dapat dihapus permanen. Gunakan fitur Nonaktifkan.");
            }
            throw Jdbc.toKlinikException(e, "Petugas", id.trim());
        }
    }

    public void toggleAktif(String id, boolean aktif) {
        if (id == null || id.trim().isEmpty()) {
            return;
        }
        String sql = "UPDATE petugas SET status_aktif = ? WHERE id_petugas = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, aktif);
            ps.setString(2, id.trim());
            if (ps.executeUpdate() == 0) {
                throw new DataNotFoundException("Petugas dengan ID " + id + " tidak ditemukan.");
            }
        } catch (SQLException e) {
            throw Jdbc.toKlinikException(e, "Petugas", id);
        }
    }

    @Override
    public Optional<Petugas> findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return Optional.empty();
        }
        String sql = "SELECT id_petugas, nama, alamat, no_telepon, username, password_hash, role, id_dokter, status_aktif "
                   + "FROM petugas WHERE id_petugas = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw Jdbc.toKlinikException(e, "Petugas", id);
        }
        return Optional.empty();
    }

    @Override
    public List<Petugas> findAll() {
        List<Petugas> list = new ArrayList<>();
        String sql = "SELECT id_petugas, nama, alamat, no_telepon, username, password_hash, role, id_dokter, status_aktif "
                   + "FROM petugas ORDER BY id_petugas";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw Jdbc.toKlinikException(e, "Petugas", null);
        }
        return list;
    }

    public List<Petugas> findActive() {
        List<Petugas> list = new ArrayList<>();
        String sql = "SELECT id_petugas, nama, alamat, no_telepon, username, password_hash, role, id_dokter, status_aktif "
                   + "FROM petugas WHERE status_aktif = TRUE ORDER BY id_petugas";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw Jdbc.toKlinikException(e, "Petugas", null);
        }
        return list;
    }

    /**
     * Mencari petugas berdasarkan username (case-insensitive).
     */
    public Optional<Petugas> cariByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return Optional.empty();
        }
        String sql = "SELECT id_petugas, nama, alamat, no_telepon, username, password_hash, role, id_dokter, status_aktif "
                   + "FROM petugas WHERE lower(username) = lower(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw Jdbc.toKlinikException(e, "Petugas", username);
        }
        return Optional.empty();
    }

    public Optional<Petugas> findByUsername(String username) {
        return cariByUsername(username);
    }

    /**
     * BR-01: Memvalidasi bahwa objek petugas ditemukan dan memiliki status aktif (status_aktif = true).
     */
    public void validasiStatusAktif(Petugas petugas) throws DataNotFoundException, DataTidakValidException {
        if (petugas == null) {
            throw new DataNotFoundException("Data petugas tidak ditemukan.");
        }
        if (!petugas.isAktif()) {
            throw new DataTidakValidException("Status Petugas",
                    "Akun petugas '" + petugas.getUsername() + "' berstatus nonaktif dan tidak dapat mengakses sistem.");
        }
    }

    /**
     * Mencari petugas berdasarkan username sekaligus memvalidasi status_aktif.
     */
    public Petugas cariAktifByUsername(String username) throws DataNotFoundException, DataTidakValidException {
        Petugas petugas = cariByUsername(username)
                .orElseThrow(() -> new DataNotFoundException("Petugas dengan username '" + username + "' tidak ditemukan."));
        validasiStatusAktif(petugas);
        return petugas;
    }

    private Petugas mapRow(ResultSet rs) throws SQLException {
        return new Petugas(
                rs.getString("id_petugas"),
                rs.getString("nama"),
                null,
                Jdbc.teks(rs, "alamat"),
                rs.getString("no_telepon"),
                rs.getBoolean("status_aktif"),
                rs.getString("username"),
                rs.getString("password_hash"),
                Role.fromString(rs.getString("role")),
                Jdbc.teks(rs, "id_dokter")
        );
    }

    private void ensurePasswordHashed(Petugas petugas) {
        String pwd = petugas.getPassword();
        if (pwd != null && !pwd.isEmpty() && !pwd.contains(":")) {
            petugas.setPassword(PasswordUtil.hashPassword(pwd));
        }
    }
}

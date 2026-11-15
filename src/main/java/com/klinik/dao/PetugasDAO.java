package com.klinik.dao;

import com.klinik.exception.DataNotFoundException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.model.Petugas;
import com.klinik.model.Role;
import com.klinik.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO akun pengguna di PostgreSQL (tabel petugas). Akun dokter berupa baris dengan role DOKTER
 * yang menunjuk ke dokter lewat id_dokter. Akun awal dibuat oleh seed.sql.
 */
public class PetugasDAO implements GenericDAO<Petugas, String> {

    private static final String SELECT_DASAR =
            "SELECT id_petugas, nama, alamat, no_telepon, username, password_hash, role, id_dokter, status_aktif "
          + "FROM petugas ";
    private static final String URUTAN = " ORDER BY id_petugas";

    static final String SQL_SEMUA   = SELECT_DASAR + URUTAN;
    static final String SQL_AKTIF   = SELECT_DASAR + "WHERE status_aktif" + URUTAN;
    static final String SQL_BY_ID   = SELECT_DASAR + "WHERE id_petugas = ?";
    static final String SQL_BY_USERNAME = SELECT_DASAR + "WHERE lower(username) = lower(?)";
    static final String SQL_INSERT =
            "INSERT INTO petugas (id_petugas, nama, alamat, no_telepon, username, password_hash, role, id_dokter, status_aktif) "
          + "VALUES (COALESCE(?, fn_id('PTG', nextval('seq_petugas'))), ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id_petugas";
    static final String SQL_UPDATE =
            "UPDATE petugas SET nama = ?, alamat = ?, no_telepon = ?, username = ?, password_hash = ?, role = ?, "
          + "id_dokter = ?, status_aktif = ? WHERE id_petugas = ?";
    static final String SQL_DELETE    = "DELETE FROM petugas WHERE id_petugas = ?";
    static final String SQL_AKTIF_SET = "UPDATE petugas SET status_aktif = ? WHERE id_petugas = ?";

    public PetugasDAO() { }

    public String generateNextId() {
        return Jdbc.idBerikut("PTG", "seq_petugas");
    }

    @Override
    public void insert(Petugas p) {
        if (p == null) return;
        pastikanTerhash(p);
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(SQL_INSERT)) {
            if (p.getId() == null || p.getId().trim().isEmpty()) {
                ps.setNull(1, Types.VARCHAR);
            } else {
                ps.setString(1, p.getId().trim());
            }
            isiKolom(ps, p, 2);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                p.setId(rs.getString(1));
            }
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
        }
    }

    @Override
    public void update(Petugas p) {
        if (p == null || p.getId() == null) return;
        pastikanTerhash(p);
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(SQL_UPDATE)) {
            isiKolom(ps, p, 1);
            ps.setString(9, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
        }
    }

    /** Urutan: nama, alamat, no_telepon, username, password_hash, role, id_dokter, status_aktif. */
    private void isiKolom(PreparedStatement ps, Petugas p, int mulai) throws SQLException {
        int i = mulai;
        ps.setString(i++, p.getNama());
        ps.setString(i++, p.getAlamat());
        ps.setString(i++, p.getNoTelepon());
        ps.setString(i++, p.getUsername());
        ps.setString(i++, p.getPassword());
        ps.setString(i++, (p.getRole() != null ? p.getRole() : Role.PETUGAS).name());
        Jdbc.teksAtauNull(ps, i++, p.getIdDokter());
        ps.setBoolean(i, p.isAktif());
    }

    @Override
    public void delete(String id) {
        if (id == null) return;
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(SQL_DELETE)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
        }
    }

    public void toggleAktif(String id, boolean aktif) {
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(SQL_AKTIF_SET)) {
            ps.setBoolean(1, aktif);
            ps.setString(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
        }
    }

    @Override
    public Optional<Petugas> findById(String id) {
        if (id == null) return Optional.empty();
        return satu(SQL_BY_ID, id);
    }

    @Override
    public List<Petugas> findAll() {
        return daftar(SQL_SEMUA);
    }

    public List<Petugas> findActive() {
        return daftar(SQL_AKTIF);
    }

    /** Mencari petugas berdasarkan username (tidak peka huruf besar-kecil). */
    public Optional<Petugas> cariByUsername(String username) {
        if (username == null || username.trim().isEmpty()) return Optional.empty();
        return satu(SQL_BY_USERNAME, username.trim());
    }

    public Optional<Petugas> findByUsername(String username) {
        return cariByUsername(username);
    }

    /** BR-01: akun nonaktif ditolak. */
    public void validasiStatusAktif(Petugas petugas) throws DataNotFoundException, DataTidakValidException {
        if (petugas == null) {
            throw new DataNotFoundException("Data petugas tidak ditemukan.");
        }
        if (!petugas.isAktif()) {
            throw new DataTidakValidException("Status Petugas",
                    "Akun petugas '" + petugas.getUsername() + "' berstatus nonaktif dan tidak dapat mengakses sistem.");
        }
    }

    public Petugas cariAktifByUsername(String username) throws DataNotFoundException, DataTidakValidException {
        Petugas petugas = cariByUsername(username)
                .orElseThrow(() -> new DataNotFoundException("Petugas dengan username '" + username + "' tidak ditemukan."));
        validasiStatusAktif(petugas);
        return petugas;
    }

    private void pastikanTerhash(Petugas petugas) {
        String pwd = petugas.getPassword();
        if (pwd != null && !pwd.isEmpty() && !pwd.contains(":")) {
            petugas.setPassword(PasswordUtil.hashPassword(pwd));
        }
    }

    private Optional<Petugas> satu(String sql, String parameter) {
        List<Petugas> hasil = daftar(sql, parameter);
        return hasil.isEmpty() ? Optional.empty() : Optional.of(hasil.get(0));
    }

    private List<Petugas> daftar(String sql, String... parameter) {
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < parameter.length; i++) ps.setString(i + 1, parameter[i]);
            List<Petugas> hasil = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    hasil.add(new Petugas(
                            rs.getString("id_petugas"),
                            rs.getString("nama"),
                            null,                                   // tabel petugas tidak menyimpan jenis kelamin
                            Jdbc.teks(rs, "alamat"),
                            rs.getString("no_telepon"),
                            rs.getBoolean("status_aktif"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            Role.fromString(rs.getString("role")),
                            Jdbc.teks(rs, "id_dokter")));
                }
            }
            return hasil;
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
        }
    }
}

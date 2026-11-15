package com.klinik.dao;

import com.klinik.exception.DataMasihDipakaiException;
import com.klinik.model.Dokter;
import com.klinik.model.JenisKelamin;
import com.klinik.util.PesanDb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** DAO dokter di PostgreSQL (tabel dokter). Signature publik sama dengan versi berbasis file sebelumnya. */
public class DokterDAO implements GenericDAO<Dokter, String> {

    private static final String SELECT_DASAR =
            "SELECT id_dokter, nama, jenis_kelamin, alamat, no_telepon, nomor_registrasi, bidang_pelayanan, status_aktif "
          + "FROM dokter ";
    private static final String URUTAN = " ORDER BY id_dokter";

    static final String SQL_SEMUA  = SELECT_DASAR + URUTAN;
    static final String SQL_AKTIF  = SELECT_DASAR + "WHERE status_aktif" + URUTAN;
    static final String SQL_BY_ID  = SELECT_DASAR + "WHERE id_dokter = ?";
    static final String SQL_CARI_NAMA   = SELECT_DASAR + "WHERE lower(nama) LIKE ?" + URUTAN;
    static final String SQL_CARI_BIDANG = SELECT_DASAR + "WHERE lower(bidang_pelayanan) LIKE ?" + URUTAN;
    static final String SQL_CARI_SEMUA  = SELECT_DASAR
            + "WHERE lower(nama) LIKE ? OR lower(id_dokter) LIKE ? OR lower(bidang_pelayanan) LIKE ?" + URUTAN;
    static final String SQL_INSERT =
            "INSERT INTO dokter (id_dokter, nama, jenis_kelamin, alamat, no_telepon, nomor_registrasi, bidang_pelayanan, status_aktif) "
          + "VALUES (COALESCE(?, fn_id('DKT', nextval('seq_dokter'))), ?, ?, ?, ?, ?, ?, ?) RETURNING id_dokter";
    static final String SQL_UPDATE =
            "UPDATE dokter SET nama = ?, jenis_kelamin = ?, alamat = ?, no_telepon = ?, nomor_registrasi = ?, "
          + "bidang_pelayanan = ?, status_aktif = ? WHERE id_dokter = ?";
    static final String SQL_DELETE    = "DELETE FROM dokter WHERE id_dokter = ?";
    static final String SQL_AKTIF_SET = "UPDATE dokter SET status_aktif = ? WHERE id_dokter = ?";

    public DokterDAO() { }

    public String generateNextId() {
        return Jdbc.idBerikut("DKT", "seq_dokter");
    }

    @Override
    public void insert(Dokter d) {
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(SQL_INSERT)) {
            if (d.getId() == null || d.getId().trim().isEmpty()) {
                ps.setNull(1, Types.VARCHAR);
            } else {
                ps.setString(1, d.getId().trim());
            }
            isiKolom(ps, d, 2);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                d.setId(rs.getString(1));
            }
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
        }
    }

    @Override
    public void update(Dokter d) {
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(SQL_UPDATE)) {
            isiKolom(ps, d, 1);
            ps.setString(8, d.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
        }
    }

    /** Urutan: nama, jenis_kelamin, alamat, no_telepon, nomor_registrasi, bidang_pelayanan, status_aktif. */
    private void isiKolom(PreparedStatement ps, Dokter d, int mulai) throws SQLException {
        int i = mulai;
        ps.setString(i++, d.getNama());
        if (d.getJenisKelamin() == null) ps.setNull(i++, Types.VARCHAR); else ps.setString(i++, d.getJenisKelamin().getLabel());
        ps.setString(i++, d.getAlamat());
        ps.setString(i++, d.getNoTelepon());
        ps.setString(i++, d.getNomorRegistrasi());
        ps.setString(i++, d.getBidangPelayanan());
        ps.setBoolean(i, d.isAktif());
    }

    /** BR-11: dokter yang sudah punya riwayat ditolak oleh foreign key dan dilaporkan sebagai DataMasihDipakaiException. */
    @Override
    public void delete(String id) {
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(SQL_DELETE)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (PesanDb.dataDipakai(e)) {
                throw new DataMasihDipakaiException("Dokter", id,
                        "Dokter " + id + " masih dipakai (memiliki riwayat pemeriksaan atau akun) dan tidak dapat dihapus permanen. Gunakan fitur Nonaktifkan.");
            }
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
    public Optional<Dokter> findById(String id) {
        if (id == null) return Optional.empty();
        List<Dokter> hasil = daftar(SQL_BY_ID, id);
        return hasil.isEmpty() ? Optional.empty() : Optional.of(hasil.get(0));
    }

    /** Dipakai DAO lain agar satu operasi memakai satu koneksi. Mengembalikan null bila tidak ada. */
    Dokter muat(Connection c, String id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SQL_BY_ID)) {
            ps.setString(1, id);
            List<Dokter> hasil = baca(ps);
            return hasil.isEmpty() ? null : hasil.get(0);
        }
    }

    @Override
    public List<Dokter> findAll() {
        return daftar(SQL_SEMUA);
    }

    public List<Dokter> findActive() {
        return daftar(SQL_AKTIF);
    }

    public List<Dokter> search(String keyword, String criteria) {
        if (keyword == null || keyword.trim().isEmpty()) return findAll();
        String kw = Jdbc.like(keyword);
        if ("Bidang Pelayanan".equalsIgnoreCase(criteria)) return daftar(SQL_CARI_BIDANG, kw);
        if ("Nama".equalsIgnoreCase(criteria)) return daftar(SQL_CARI_NAMA, kw);
        return daftar(SQL_CARI_SEMUA, kw, kw, kw);
    }

    private List<Dokter> daftar(String sql, String... parameter) {
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < parameter.length; i++) ps.setString(i + 1, parameter[i]);
            return baca(ps);
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
        }
    }

    private List<Dokter> baca(PreparedStatement ps) throws SQLException {
        List<Dokter> hasil = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                hasil.add(new Dokter(
                        rs.getString("id_dokter"),
                        rs.getString("nama"),
                        JenisKelamin.fromString(rs.getString("jenis_kelamin")),
                        Jdbc.teks(rs, "alamat"),
                        rs.getString("no_telepon"),
                        rs.getString("nomor_registrasi"),
                        rs.getString("bidang_pelayanan"),
                        rs.getBoolean("status_aktif")));
            }
        }
        return hasil;
    }
}

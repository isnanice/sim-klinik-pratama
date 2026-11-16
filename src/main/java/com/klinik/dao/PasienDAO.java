package com.klinik.dao;

import com.klinik.exception.DataMasihDipakaiException;
import com.klinik.exception.DataNotFoundException;
import com.klinik.model.JenisKelamin;
import com.klinik.model.Pasien;
import com.klinik.util.PesanDb;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** DAO pasien di PostgreSQL (tabel pasien). Signature publik sama dengan versi berbasis file sebelumnya. */
public class PasienDAO implements GenericDAO<Pasien, String> {

    private static final String SELECT_DASAR =
            "SELECT id_pasien, nik, no_bpjs, nama, jenis_kelamin, tanggal_lahir, alamat, no_telepon, "
          + "tanggal_registrasi, status_aktif FROM pasien ";
    private static final String URUTAN = " ORDER BY id_pasien";

    static final String SQL_SEMUA   = SELECT_DASAR + URUTAN;
    static final String SQL_AKTIF   = SELECT_DASAR + "WHERE status_aktif" + URUTAN;
    static final String SQL_BY_ID   = SELECT_DASAR + "WHERE id_pasien = ?";
    static final String SQL_BY_NIK  = SELECT_DASAR + "WHERE nik = ?";
    static final String SQL_BY_NAMA = SELECT_DASAR + "WHERE lower(nama) LIKE ?" + URUTAN;
    static final String SQL_CARI_ID   = SELECT_DASAR + "WHERE lower(id_pasien) LIKE ?" + URUTAN;
    static final String SQL_CARI_NIK  = SELECT_DASAR + "WHERE nik LIKE ?" + URUTAN;
    static final String SQL_CARI_BPJS = SELECT_DASAR + "WHERE COALESCE(no_bpjs, '') LIKE ?" + URUTAN;
    static final String SQL_CARI_SEMUA = SELECT_DASAR
            + "WHERE lower(id_pasien) LIKE ? OR nik LIKE ? OR lower(nama) LIKE ? OR COALESCE(no_bpjs, '') LIKE ?" + URUTAN;
    static final String SQL_INSERT =
            "INSERT INTO pasien (id_pasien, nik, no_bpjs, nama, jenis_kelamin, tanggal_lahir, alamat, no_telepon, "
          + "tanggal_registrasi, status_aktif) "
          + "VALUES (COALESCE(?, fn_id('PSN', nextval('seq_pasien'))), ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id_pasien";
    static final String SQL_UPDATE =
            "UPDATE pasien SET nik = ?, no_bpjs = ?, nama = ?, jenis_kelamin = ?, tanggal_lahir = ?, alamat = ?, "
          + "no_telepon = ?, tanggal_registrasi = ?, status_aktif = ? WHERE id_pasien = ?";
    static final String SQL_DELETE  = "DELETE FROM pasien WHERE id_pasien = ?";
    static final String SQL_AKTIF_SET = "UPDATE pasien SET status_aktif = ? WHERE id_pasien = ?";
    static final String SQL_ADA_RIWAYAT = "SELECT EXISTS (SELECT 1 FROM pemeriksaan WHERE id_pasien = ?)";

    public PasienDAO() { }

    /** Dipertahankan agar kode lama tetap kompilasi; riwayat kini dicek langsung lewat SQL. */
    public PasienDAO(PemeriksaanDAO pemeriksaanDAO) { }

    public void setPemeriksaanDAO(PemeriksaanDAO pemeriksaanDAO) { }

    public String generateNextId() {
        return Jdbc.idBerikut("PSN", "seq_pasien");
    }

    // ------------------------------------------------------------------ tulis

    @Override
    public void insert(Pasien p) {
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

    public void simpan(Pasien pasien) {
        insert(pasien);
    }

    @Override
    public void update(Pasien p) {
        if (p == null || p.getId() == null) return;
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(SQL_UPDATE)) {
            isiKolom(ps, p, 1);
            ps.setString(10, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
        }
    }

    public void ubah(Pasien pasien) {
        update(pasien);
    }

    /** Urutan parameter: nik, no_bpjs, nama, jenis_kelamin, tanggal_lahir, alamat, no_telepon, tanggal_registrasi, status_aktif. */
    private void isiKolom(PreparedStatement ps, Pasien p, int mulai) throws SQLException {
        int i = mulai;
        ps.setString(i++, p.getNik() == null ? "" : p.getNik());
        Jdbc.teksAtauNull(ps, i++, p.getNoBpjs());
        ps.setString(i++, p.getNama());
        if (p.getJenisKelamin() == null) ps.setNull(i++, Types.VARCHAR); else ps.setString(i++, p.getJenisKelamin().getLabel());
        if (p.getTanggalLahir() == null) ps.setNull(i++, Types.DATE); else ps.setDate(i++, Date.valueOf(p.getTanggalLahir()));
        ps.setString(i++, p.getAlamat());
        ps.setString(i++, p.getNoTelepon());
        ps.setDate(i++, Date.valueOf(p.getTanggalRegistrasi()));
        ps.setBoolean(i, p.isAktif());
    }

    /** BR-11: hapus permanen hanya bila belum punya riwayat pemeriksaan. */
    @Override
    public void delete(String id) throws DataMasihDipakaiException {
        if (id == null) return;
        if (hasRiwayatPemeriksaan(id)) {
            String nama = findById(id).map(Pasien::getNama).orElse(id);
            throw new DataMasihDipakaiException("Pasien", id, "Pasien " + nama + " (" + id
                    + ") memiliki riwayat pemeriksaan dan tidak dapat dihapus permanen. Gunakan fitur Nonaktifkan.");
        }
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(SQL_DELETE)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (PesanDb.dataDipakai(e)) {
                throw new DataMasihDipakaiException("Pasien", id,
                        "Pasien " + id + " memiliki riwayat pemeriksaan dan tidak dapat dihapus permanen. Gunakan fitur Nonaktifkan.");
            }
            throw Jdbc.gagal(e);
        }
    }

    public void hapus(String id) throws DataMasihDipakaiException {
        delete(id);
    }

    public boolean hasRiwayatPemeriksaan(String pasienId) {
        if (pasienId == null || pasienId.trim().isEmpty()) return false;
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(SQL_ADA_RIWAYAT)) {
            ps.setString(1, pasienId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
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

    public void nonaktifkanPasien(String id) throws DataNotFoundException {
        if (findById(id).isEmpty()) {
            throw new DataNotFoundException("Pasien dengan ID " + id + " tidak ditemukan.");
        }
        toggleAktif(id, false);
    }

    // ------------------------------------------------------------------ baca

    @Override
    public Optional<Pasien> findById(String id) {
        if (id == null) return Optional.empty();
        List<Pasien> hasil = daftar(SQL_BY_ID, id);
        return hasil.isEmpty() ? Optional.empty() : Optional.of(hasil.get(0));
    }

    /** Dipakai DAO lain agar satu operasi memakai satu koneksi. Mengembalikan null bila tidak ada. */
    Pasien muat(Connection c, String id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SQL_BY_ID)) {
            ps.setString(1, id);
            List<Pasien> hasil = baca(ps);
            return hasil.isEmpty() ? null : hasil.get(0);
        }
    }

    public Optional<Pasien> findByNik(String nik) {
        if (nik == null) return Optional.empty();
        List<Pasien> hasil = daftar(SQL_BY_NIK, nik.trim());
        return hasil.isEmpty() ? Optional.empty() : Optional.of(hasil.get(0));
    }

    public List<Pasien> findByNama(String nama) {
        if (nama == null || nama.trim().isEmpty()) return findAll();
        return daftar(SQL_BY_NAMA, Jdbc.like(nama));
    }

    @Override
    public List<Pasien> findAll() {
        return daftar(SQL_SEMUA);
    }

    public List<Pasien> findActive() {
        return daftar(SQL_AKTIF);
    }

    /** Pencarian berdasarkan kata kunci dan kriteria ("Semua", "ID", "NIK", "Nama", "No. BPJS"). */
    public List<Pasien> search(String keyword, String criteria) {
        if (keyword == null || keyword.trim().isEmpty()) return findAll();
        String kw = Jdbc.like(keyword);
        String krit = criteria == null ? "Semua" : criteria.trim();
        if ("ID".equalsIgnoreCase(krit)) return daftar(SQL_CARI_ID, kw);
        if ("NIK".equalsIgnoreCase(krit)) return daftar(SQL_CARI_NIK, kw);
        if ("Nama".equalsIgnoreCase(krit)) return daftar(SQL_BY_NAMA, kw);
        if ("No. BPJS".equalsIgnoreCase(krit) || "BPJS".equalsIgnoreCase(krit)) return daftar(SQL_CARI_BPJS, kw);
        return daftar(SQL_CARI_SEMUA, kw, kw, kw, kw);
    }

    public List<Pasien> cari(String keyword) {
        return search(keyword, "Semua");
    }

    private List<Pasien> daftar(String sql, String... parameter) {
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < parameter.length; i++) ps.setString(i + 1, parameter[i]);
            return baca(ps);
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
        }
    }

    private List<Pasien> baca(PreparedStatement ps) throws SQLException {
        List<Pasien> hasil = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                hasil.add(new Pasien(
                        rs.getString("id_pasien"),
                        rs.getString("nik"),
                        rs.getString("nama"),
                        JenisKelamin.fromString(rs.getString("jenis_kelamin")),
                        Jdbc.tanggal(rs, "tanggal_lahir"),
                        Jdbc.teks(rs, "alamat"),
                        rs.getString("no_telepon"),
                        Jdbc.teks(rs, "no_bpjs"),
                        Jdbc.tanggal(rs, "tanggal_registrasi"),
                        rs.getBoolean("status_aktif")));
            }
        }
        return hasil;
    }
}

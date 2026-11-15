package com.klinik.dao;

import com.klinik.model.Layanan;
import com.klinik.model.LayananSpesialis;
import com.klinik.model.LayananUmum;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO layanan di PostgreSQL (tabel layanan). Objek dibuat sesuai kolom jenis_layanan
 * (UMUM atau SPESIALIS) sehingga pemanggil menerima tipe Layanan yang polimorfik.
 * Layanan tidak dihapus permanen, hanya dinonaktifkan (BR-11).
 */
public class LayananDAO implements GenericDAO<Layanan, String> {

    private static final String SELECT_DASAR =
            "SELECT id_layanan, nama_layanan, jenis_layanan, tarif, keterangan, status_aktif FROM layanan ";
    private static final String URUTAN = " ORDER BY id_layanan";

    static final String SQL_SEMUA = SELECT_DASAR + URUTAN;
    static final String SQL_AKTIF = SELECT_DASAR + "WHERE status_aktif" + URUTAN;
    static final String SQL_BY_ID = SELECT_DASAR + "WHERE id_layanan = ?";
    static final String SQL_CARI  = SELECT_DASAR
            + "WHERE lower(nama_layanan) LIKE ? OR lower(id_layanan) LIKE ? OR lower(jenis_layanan) LIKE ?" + URUTAN;
    static final String SQL_INSERT =
            "INSERT INTO layanan (id_layanan, nama_layanan, jenis_layanan, tarif, keterangan, status_aktif) "
          + "VALUES (COALESCE(?, fn_id('LYN', nextval('seq_layanan'))), ?, ?, ?, ?, ?) RETURNING id_layanan";
    static final String SQL_UPDATE =
            "UPDATE layanan SET nama_layanan = ?, jenis_layanan = ?, tarif = ?, keterangan = ?, status_aktif = ? "
          + "WHERE id_layanan = ?";
    static final String SQL_AKTIF_SET = "UPDATE layanan SET status_aktif = ? WHERE id_layanan = ?";

    public LayananDAO() { }

    public String generateNextId() {
        return Jdbc.idBerikut("LYN", "seq_layanan");
    }

    @Override
    public void insert(Layanan l) {
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(SQL_INSERT)) {
            if (l.getId() == null || l.getId().trim().isEmpty()) {
                ps.setNull(1, Types.VARCHAR);
            } else {
                ps.setString(1, l.getId().trim());
            }
            ps.setString(2, l.getNamaLayanan());
            ps.setString(3, jenis(l));
            ps.setDouble(4, l.getTarifDasar());
            Jdbc.teksAtauNull(ps, 5, l.getKeterangan());
            ps.setBoolean(6, l.isAktif());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                l.setId(rs.getString(1));
            }
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
        }
    }

    @Override
    public void update(Layanan l) {
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, l.getNamaLayanan());
            ps.setString(2, jenis(l));
            ps.setDouble(3, l.getTarifDasar());
            Jdbc.teksAtauNull(ps, 4, l.getKeterangan());
            ps.setBoolean(5, l.isAktif());
            ps.setString(6, l.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
        }
    }

    private static String jenis(Layanan l) {
        return l instanceof LayananSpesialis ? "SPESIALIS" : "UMUM";
    }

    /** Layanan dalam aturan bisnis tidak dihapus permanen melainkan dinonaktifkan. */
    @Override
    public void delete(String id) {
        toggleAktif(id, false);
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
    public Optional<Layanan> findById(String id) {
        if (id == null) return Optional.empty();
        List<Layanan> hasil = daftar(SQL_BY_ID, id);
        return hasil.isEmpty() ? Optional.empty() : Optional.of(hasil.get(0));
    }

    /** Dipakai DAO lain agar satu operasi memakai satu koneksi. Mengembalikan null bila tidak ada. */
    Layanan muat(Connection c, String id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SQL_BY_ID)) {
            ps.setString(1, id);
            List<Layanan> hasil = baca(ps);
            return hasil.isEmpty() ? null : hasil.get(0);
        }
    }

    @Override
    public List<Layanan> findAll() {
        return daftar(SQL_SEMUA);
    }

    public List<Layanan> findActive() {
        return daftar(SQL_AKTIF);
    }

    public List<Layanan> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return findAll();
        String kw = Jdbc.like(keyword);
        return daftar(SQL_CARI, kw, kw, kw);
    }

    private List<Layanan> daftar(String sql, String... parameter) {
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < parameter.length; i++) ps.setString(i + 1, parameter[i]);
            return baca(ps);
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
        }
    }

    private List<Layanan> baca(PreparedStatement ps) throws SQLException {
        List<Layanan> hasil = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString("id_layanan");
                String nama = rs.getString("nama_layanan");
                double tarif = rs.getDouble("tarif");
                String keterangan = Jdbc.teks(rs, "keterangan");
                boolean aktif = rs.getBoolean("status_aktif");
                if ("SPESIALIS".equalsIgnoreCase(rs.getString("jenis_layanan"))) {
                    hasil.add(new LayananSpesialis(id, nama, tarif, keterangan, aktif));   // biaya konsultasi = konstanta kelas
                } else {
                    hasil.add(new LayananUmum(id, nama, tarif, keterangan, aktif));
                }
            }
        }
        return hasil;
    }
}

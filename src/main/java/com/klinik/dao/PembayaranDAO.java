package com.klinik.dao;

import com.klinik.exception.DataTidakValidException;
import com.klinik.model.MetodePembayaran;
import com.klinik.model.Pembayaran;
import com.klinik.model.StatusPembayaran;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO pembayaran di PostgreSQL. Penyimpanan pembayaran memakai satu transaksi (BR-12):
 * kunjungan dikunci (FOR UPDATE), status SELESAI dan belum pernah dibayar diperiksa ulang,
 * lalu pembayaran disimpan; commit bila semua berhasil, rollback bila ada yang gagal.
 */
public class PembayaranDAO implements GenericDAO<Pembayaran, String> {

    private static final String SELECT_DASAR =
            "SELECT id_pembayaran, id_pemeriksaan, tanggal_pembayaran, biaya_tambahan, total_biaya, "
          + "metode_pembayaran, status_pembayaran FROM pembayaran ";

    static final String SQL_SEMUA          = SELECT_DASAR + "ORDER BY tanggal_pembayaran DESC, id_pembayaran DESC";
    static final String SQL_BY_ID          = SELECT_DASAR + "WHERE id_pembayaran = ?";
    static final String SQL_BY_PEMERIKSAAN = SELECT_DASAR + "WHERE id_pemeriksaan = ?";
    static final String SQL_KUNCI_KUNJUNGAN =
            "SELECT status FROM pemeriksaan WHERE id_pemeriksaan = ? FOR UPDATE";
    static final String SQL_SUDAH_DIBAYAR  = "SELECT EXISTS (SELECT 1 FROM pembayaran WHERE id_pemeriksaan = ?)";
    static final String SQL_INSERT =
            "INSERT INTO pembayaran (id_pembayaran, id_pemeriksaan, tanggal_pembayaran, biaya_tambahan, total_biaya, "
          + "metode_pembayaran, status_pembayaran) "
          + "VALUES (COALESCE(?, fn_id('BYR', nextval('seq_pembayaran'))), ?, ?, ?, ?, ?, ?) RETURNING id_pembayaran";
    static final String SQL_UPDATE =
            "UPDATE pembayaran SET metode_pembayaran = ?, status_pembayaran = ? WHERE id_pembayaran = ?";
    static final String SQL_DELETE = "DELETE FROM pembayaran WHERE id_pembayaran = ?";

    private final PemeriksaanDAO pemeriksaanDAO;

    public PembayaranDAO(PemeriksaanDAO pemeriksaanDAO) {
        this.pemeriksaanDAO = pemeriksaanDAO;
    }

    public String generateNextId() {
        return Jdbc.idBerikut("BYR", "seq_pembayaran");
    }

    /** BR-12: transaksi tunggal (setAutoCommit(false), commit atau rollback). */
    @Override
    public void insert(Pembayaran b) {
        try (Connection c = Jdbc.buka()) {
            c.setAutoCommit(false);
            try {
                String idPemeriksaan = b.getPemeriksaan().getId();
                try (PreparedStatement ps = c.prepareStatement(SQL_KUNCI_KUNJUNGAN)) {
                    ps.setString(1, idPemeriksaan);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || !"SELESAI".equals(rs.getString(1))) {
                            throw new DataTidakValidException("Status Pemeriksaan",
                                    "Pembayaran hanya dapat diproses untuk pemeriksaan yang berstatus SELESAI.");
                        }
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(SQL_SUDAH_DIBAYAR)) {
                    ps.setString(1, idPemeriksaan);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getBoolean(1)) {
                            throw new DataTidakValidException("Pembayaran", "Pemeriksaan ini sudah memiliki data pembayaran.");
                        }
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(SQL_INSERT)) {
                    if (b.getId() == null || b.getId().trim().isEmpty()) {
                        ps.setNull(1, Types.VARCHAR);
                    } else {
                        ps.setString(1, b.getId().trim());
                    }
                    ps.setString(2, idPemeriksaan);
                    ps.setDate(3, Date.valueOf(b.getTanggalPembayaran()));
                    ps.setDouble(4, b.getBiayaTambahan());
                    ps.setDouble(5, b.getTotalBiaya());
                    ps.setString(6, b.getMetodePembayaran().name());
                    ps.setString(7, b.getStatusPembayaran().name());
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        b.setId(rs.getString(1));
                    }
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
        }
    }

    /** Mengubah metode dan status. Trigger database menolak perubahan pada pembayaran yang sudah LUNAS. */
    @Override
    public void update(Pembayaran b) {
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, b.getMetodePembayaran().name());
            ps.setString(2, b.getStatusPembayaran().name());
            ps.setString(3, b.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
        }
    }

    @Override
    public void delete(String id) {
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(SQL_DELETE)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
        }
    }

    @Override
    public Optional<Pembayaran> findById(String id) {
        return satu(SQL_BY_ID, id);
    }

    public Optional<Pembayaran> findByPemeriksaanId(String idPemeriksaan) {
        return satu(SQL_BY_PEMERIKSAAN, idPemeriksaan);
    }

    @Override
    public List<Pembayaran> findAll() {
        return daftar(SQL_SEMUA, null);
    }

    private Optional<Pembayaran> satu(String sql, String id) {
        if (id == null || id.trim().isEmpty()) return Optional.empty();
        List<Pembayaran> hasil = daftar(sql, id.trim());
        return hasil.isEmpty() ? Optional.empty() : Optional.of(hasil.get(0));
    }

    private List<Pembayaran> daftar(String sql, String parameter) {
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (parameter != null) ps.setString(1, parameter);
            List<Pembayaran> hasil = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    hasil.add(new Pembayaran(
                            rs.getString("id_pembayaran"),
                            pemeriksaanDAO.findById(c, rs.getString("id_pemeriksaan")).orElse(null),
                            Jdbc.tanggal(rs, "tanggal_pembayaran"),
                            rs.getDouble("biaya_tambahan"),
                            rs.getDouble("total_biaya"),
                            MetodePembayaran.fromString(rs.getString("metode_pembayaran")),
                            StatusPembayaran.fromString(rs.getString("status_pembayaran"))));
                }
            }
            return hasil;
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
        }
    }
}

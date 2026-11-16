package com.klinik.dao;

import com.klinik.exception.DataTidakValidException;
import com.klinik.model.RekamMedis;
import com.klinik.model.StatusPemeriksaan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO rekam medis di PostgreSQL. Satu kunjungan satu rekam medis (uq_rm_pemeriksaan).
 * Pengisian tiap tahap disimpan bersama perubahan status kunjungan dalam satu transaksi
 * lewat {@link #simpanTahap}.
 */
public class RekamMedisDAO implements GenericDAO<RekamMedis, String> {

    private static final String KOLOM =
            "rm.id_rekam_medis, rm.id_pemeriksaan, rm.tensi_sistolik, rm.tensi_diastolik, rm.tinggi_badan_cm, "
          + "rm.berat_badan_kg, rm.durasi_keluhan, rm.hasil_pemeriksaan, rm.diagnosis, rm.catatan, rm.resep, "
          + "rm.obat_diberikan";

    static final String SQL_SEMUA =
            "SELECT " + KOLOM + " FROM rekam_medis rm ORDER BY rm.id_rekam_medis";
    static final String SQL_BY_ID =
            "SELECT " + KOLOM + " FROM rekam_medis rm WHERE rm.id_rekam_medis = ?";
    static final String SQL_BY_PEMERIKSAAN =
            "SELECT " + KOLOM + " FROM rekam_medis rm WHERE rm.id_pemeriksaan = ?";
    static final String SQL_BY_PASIEN =
            "SELECT " + KOLOM + " FROM rekam_medis rm JOIN pemeriksaan p ON p.id_pemeriksaan = rm.id_pemeriksaan "
          + "WHERE p.id_pasien = ? ORDER BY p.tanggal_pemeriksaan DESC, rm.id_rekam_medis DESC";
    static final String SQL_INSERT =
            "INSERT INTO rekam_medis (id_rekam_medis, id_pemeriksaan, tensi_sistolik, tensi_diastolik, "
          + "tinggi_badan_cm, berat_badan_kg, durasi_keluhan, hasil_pemeriksaan, diagnosis, catatan, resep, obat_diberikan) "
          + "VALUES (COALESCE(?, fn_id('RM', nextval('seq_rekam_medis'))), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
          + "RETURNING id_rekam_medis";
    static final String SQL_UPDATE =
            "UPDATE rekam_medis SET tensi_sistolik = ?, tensi_diastolik = ?, tinggi_badan_cm = ?, berat_badan_kg = ?, "
          + "durasi_keluhan = ?, hasil_pemeriksaan = ?, diagnosis = ?, catatan = ?, resep = ?, obat_diberikan = ? "
          + "WHERE id_rekam_medis = ?";
    static final String SQL_STATUS =
            "UPDATE pemeriksaan SET status = ? WHERE id_pemeriksaan = ? AND status = ?";
    static final String SQL_DELETE = "DELETE FROM rekam_medis WHERE id_rekam_medis = ?";

    private final PemeriksaanDAO pemeriksaanDAO;

    public RekamMedisDAO(PemeriksaanDAO pemeriksaanDAO) {
        this.pemeriksaanDAO = pemeriksaanDAO;
    }

    public String generateNextId() {
        return Jdbc.idBerikut("RM", "seq_rekam_medis");
    }

    // ------------------------------------------------------------------ tulis

    @Override
    public void insert(RekamMedis rm) {
        try (Connection c = Jdbc.buka()) {
            tulisBaru(c, rm);
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
        }
    }

    @Override
    public void update(RekamMedis rm) {
        try (Connection c = Jdbc.buka()) {
            tulisUbah(c, rm);
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

    /**
     * Menyimpan rekam medis (baru bila belum punya ID, selain itu diubah) dan memajukan status kunjungan
     * dari {@code dari} ke {@code ke} dalam SATU transaksi: keduanya tersimpan atau keduanya dibatalkan.
     * Pengecekan "status masih sama" mencegah dua pengguna memproses tahap yang sama sekaligus.
     */
    public void simpanTahap(RekamMedis rm, StatusPemeriksaan dari, StatusPemeriksaan ke) {
        try (Connection c = Jdbc.buka()) {
            c.setAutoCommit(false);
            try {
                if (rm.getId() == null || rm.getId().trim().isEmpty()) {
                    tulisBaru(c, rm);
                } else {
                    tulisUbah(c, rm);
                }
                try (PreparedStatement ps = c.prepareStatement(SQL_STATUS)) {
                    ps.setString(1, ke.name());
                    ps.setString(2, rm.getPemeriksaan().getId());
                    ps.setString(3, dari.name());
                    if (ps.executeUpdate() != 1) {
                        throw new DataTidakValidException("Status",
                                "Status kunjungan sudah berubah (diproses pengguna lain). Muat ulang data lalu coba lagi.");
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

    private void tulisBaru(Connection c, RekamMedis rm) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SQL_INSERT)) {
            if (rm.getId() == null || rm.getId().trim().isEmpty()) {
                ps.setNull(1, Types.VARCHAR);
            } else {
                ps.setString(1, rm.getId().trim());
            }
            ps.setString(2, rm.getPemeriksaan().getId());
            Jdbc.intAtauNull(ps, 3, rm.getTensiSistolik());
            Jdbc.intAtauNull(ps, 4, rm.getTensiDiastolik());
            Jdbc.doubleAtauNull(ps, 5, rm.getTinggiBadanCm());
            Jdbc.doubleAtauNull(ps, 6, rm.getBeratBadanKg());
            Jdbc.teksAtauNull(ps, 7, rm.getDurasiKeluhan());
            Jdbc.teksAtauNull(ps, 8, rm.getHasilPemeriksaan());
            Jdbc.teksAtauNull(ps, 9, rm.getDiagnosis());
            Jdbc.teksAtauNull(ps, 10, rm.getCatatan());
            Jdbc.teksAtauNull(ps, 11, rm.getResep());
            Jdbc.teksAtauNull(ps, 12, rm.getObatDiberikan());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                rm.setId(rs.getString(1));
            }
        }
    }

    private void tulisUbah(Connection c, RekamMedis rm) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SQL_UPDATE)) {
            Jdbc.intAtauNull(ps, 1, rm.getTensiSistolik());
            Jdbc.intAtauNull(ps, 2, rm.getTensiDiastolik());
            Jdbc.doubleAtauNull(ps, 3, rm.getTinggiBadanCm());
            Jdbc.doubleAtauNull(ps, 4, rm.getBeratBadanKg());
            Jdbc.teksAtauNull(ps, 5, rm.getDurasiKeluhan());
            Jdbc.teksAtauNull(ps, 6, rm.getHasilPemeriksaan());
            Jdbc.teksAtauNull(ps, 7, rm.getDiagnosis());
            Jdbc.teksAtauNull(ps, 8, rm.getCatatan());
            Jdbc.teksAtauNull(ps, 9, rm.getResep());
            Jdbc.teksAtauNull(ps, 10, rm.getObatDiberikan());
            ps.setString(11, rm.getId());
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------ baca

    @Override
    public Optional<RekamMedis> findById(String id) {
        return satu(SQL_BY_ID, id);
    }

    public Optional<RekamMedis> findByPemeriksaanId(String idPemeriksaan) {
        return satu(SQL_BY_PEMERIKSAAN, idPemeriksaan);
    }

    @Override
    public List<RekamMedis> findAll() {
        return daftar(SQL_SEMUA, null);
    }

    /** Riwayat rekam medis satu pasien, terbaru di atas. */
    public List<RekamMedis> findByPasienId(String idPasien) {
        if (idPasien == null || idPasien.trim().isEmpty()) return new ArrayList<>();
        return daftar(SQL_BY_PASIEN, idPasien.trim());
    }

    private Optional<RekamMedis> satu(String sql, String id) {
        if (id == null || id.trim().isEmpty()) return Optional.empty();
        List<RekamMedis> hasil = daftar(sql, id.trim());
        return hasil.isEmpty() ? Optional.empty() : Optional.of(hasil.get(0));
    }

    private List<RekamMedis> daftar(String sql, String parameter) {
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (parameter != null) ps.setString(1, parameter);
            List<RekamMedis> hasil = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RekamMedis rm = new RekamMedis();
                    rm.setId(rs.getString("id_rekam_medis"));
                    rm.setPemeriksaan(pemeriksaanDAO.findById(c, rs.getString("id_pemeriksaan")).orElse(null));
                    rm.setTensiSistolik(Jdbc.intAtauNull(rs, "tensi_sistolik"));
                    rm.setTensiDiastolik(Jdbc.intAtauNull(rs, "tensi_diastolik"));
                    rm.setTinggiBadanCm(Jdbc.doubleAtauNull(rs, "tinggi_badan_cm"));
                    rm.setBeratBadanKg(Jdbc.doubleAtauNull(rs, "berat_badan_kg"));
                    rm.setDurasiKeluhan(rs.getString("durasi_keluhan"));
                    rm.setHasilPemeriksaan(rs.getString("hasil_pemeriksaan"));
                    rm.setDiagnosis(rs.getString("diagnosis"));
                    rm.setCatatan(rs.getString("catatan"));
                    rm.setResep(rs.getString("resep"));
                    rm.setObatDiberikan(rs.getString("obat_diberikan"));
                    hasil.add(rm);
                }
            }
            return hasil;
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
        }
    }
}

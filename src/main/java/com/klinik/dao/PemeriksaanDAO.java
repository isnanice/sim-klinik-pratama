package com.klinik.dao;

import com.klinik.exception.DataNotFoundException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.model.Dokter;
import com.klinik.model.JenisPenjamin;
import com.klinik.model.Layanan;
import com.klinik.model.Pasien;
import com.klinik.model.Pemeriksaan;
import com.klinik.model.StatusPemeriksaan;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DAO pemeriksaan (kunjungan) di PostgreSQL. Seluruh query memakai PreparedStatement dan
 * try-with-resources. Nomor antrean per dokter per tanggal dibuat oleh trigger database.
 */
public class PemeriksaanDAO implements GenericDAO<Pemeriksaan, String> {

    private static final String SELECT_DASAR =
            "SELECT p.id_pemeriksaan, p.id_pasien, p.id_dokter, p.id_layanan, p.id_petugas, "
          + "pt.nama AS nama_petugas, p.tanggal_pemeriksaan, p.no_antrean, p.keluhan, p.durasi_keluhan_awal, "
          + "p.jenis_penjamin, p.status, p.jumlah_panggilan, p.alasan_batal, p.dibatalkan_oleh "
          + "FROM pemeriksaan p JOIN petugas pt ON pt.id_petugas = p.id_petugas ";
    private static final String URUTAN =
            " ORDER BY p.tanggal_pemeriksaan DESC, p.id_dokter, p.no_antrean";

    static final String SQL_SEMUA        = SELECT_DASAR + URUTAN;
    static final String SQL_BY_ID        = SELECT_DASAR + "WHERE p.id_pemeriksaan = ?";
    static final String SQL_BY_STATUS    = SELECT_DASAR + "WHERE p.status = ?" + URUTAN;
    static final String SQL_BY_DOKTER    = SELECT_DASAR + "WHERE p.id_dokter = ?" + URUTAN;
    static final String SQL_BY_PASIEN    = SELECT_DASAR + "WHERE p.id_pasien = ?" + URUTAN;
    static final String SQL_BY_TANGGAL   = SELECT_DASAR + "WHERE p.tanggal_pemeriksaan = ?" + URUTAN;
    static final String SQL_INSERT =
            "INSERT INTO pemeriksaan (id_pemeriksaan, id_pasien, id_dokter, id_layanan, id_petugas, "
          + "tanggal_pemeriksaan, no_antrean, keluhan, durasi_keluhan_awal, jenis_penjamin, status) "
          + "VALUES (COALESCE(?, fn_id('PMR', nextval('seq_pemeriksaan'))), ?, ?, ?, ?, ?, NULL, ?, ?, ?, ?) "
          + "RETURNING id_pemeriksaan, no_antrean";
    static final String SQL_UPDATE =
            "UPDATE pemeriksaan SET status = ?, jumlah_panggilan = ?, alasan_batal = ?, dibatalkan_oleh = ? "
          + "WHERE id_pemeriksaan = ?";
    static final String SQL_PANGGIL =
            "UPDATE pemeriksaan SET jumlah_panggilan = jumlah_panggilan + 1 "
          + "WHERE id_pemeriksaan = ? AND status NOT IN ('SELESAI', 'DIBATALKAN') RETURNING jumlah_panggilan";
    static final String SQL_DELETE       = "DELETE FROM pemeriksaan WHERE id_pemeriksaan = ?";
    static final String SQL_ADA_DOKTER   = "SELECT EXISTS (SELECT 1 FROM pemeriksaan WHERE id_dokter = ?)";
    static final String SQL_ADA_PASIEN   = "SELECT EXISTS (SELECT 1 FROM pemeriksaan WHERE id_pasien = ?)";

    private final PasienDAO pasienDAO;
    private final DokterDAO dokterDAO;
    private final LayananDAO layananDAO;

    public PemeriksaanDAO(PasienDAO pasienDAO, DokterDAO dokterDAO, LayananDAO layananDAO) {
        this.pasienDAO = pasienDAO;
        this.dokterDAO = dokterDAO;
        this.layananDAO = layananDAO;
    }

    public String generateNextId() {
        return Jdbc.idBerikut("PMR", "seq_pemeriksaan");
    }

    // ------------------------------------------------------------------ tulis

    /** Menyimpan kunjungan baru. ID dan nomor antrean diisi dari database ke objek yang dikirim. */
    @Override
    public void insert(Pemeriksaan p) {
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(SQL_INSERT)) {
            if (p.getId() == null || p.getId().trim().isEmpty()) {
                ps.setNull(1, Types.VARCHAR);
            } else {
                ps.setString(1, p.getId().trim());
            }
            ps.setString(2, p.getPasien().getId());
            ps.setString(3, p.getDokter().getId());
            ps.setString(4, p.getLayanan().getId());
            ps.setString(5, p.getIdPetugas());
            ps.setDate(6, Date.valueOf(p.getTanggalPemeriksaan()));
            ps.setString(7, p.getKeluhanAwal().trim());
            ps.setString(8, p.getDurasiKeluhan().trim());
            ps.setString(9, p.getJenisPenjamin().name());
            ps.setString(10, p.getStatus().name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                p.setId(rs.getString("id_pemeriksaan"));
                p.setNoAntrean(rs.getInt("no_antrean"));
            }
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
        }
    }

    /** Memperbarui status, jumlah panggilan, dan data pembatalan. Aturan status dijaga model dan trigger. */
    @Override
    public void update(Pemeriksaan p) {
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, p.getStatus().name());
            ps.setInt(2, p.getJumlahPanggilan());
            Jdbc.teksAtauNull(ps, 3, p.getStatus() == StatusPemeriksaan.DIBATALKAN ? p.getAlasanBatal() : null);
            Jdbc.teksAtauNull(ps, 4, p.getStatus() == StatusPemeriksaan.DIBATALKAN ? p.getDibatalkanOleh() : null);
            ps.setString(5, p.getId());
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

    /** BR-14: menambah jumlah panggilan secara atomik pada tahap yang sedang berjalan. */
    public Pemeriksaan panggilAntrean(String idPemeriksaan) throws DataNotFoundException, DataTidakValidException {
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(SQL_PANGGIL)) {
            ps.setString(1, idPemeriksaan);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    Pemeriksaan ada = findById(idPemeriksaan)
                            .orElseThrow(() -> new DataNotFoundException("Data pemeriksaan " + idPemeriksaan + " tidak ditemukan."));
                    throw new DataTidakValidException("Status",
                            "Kunjungan berstatus " + ada.getStatus().getLabel() + " tidak dapat dipanggil lagi.");
                }
            }
            return findById(c, idPemeriksaan).orElseThrow(() -> new DataNotFoundException("Data pemeriksaan tidak ditemukan."));
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
        }
    }

    /** BR-15: model memeriksa batas panggilan dan alasan, lalu hasilnya disimpan. */
    public Pemeriksaan batalkanAntrean(String idPemeriksaan, String alasan, String dibatalkanOleh)
            throws DataNotFoundException, DataTidakValidException {
        Pemeriksaan p = findById(idPemeriksaan)
                .orElseThrow(() -> new DataNotFoundException("Data pemeriksaan " + idPemeriksaan + " tidak ditemukan."));
        p.batalkan(alasan, dibatalkanOleh);
        update(p);
        return p;
    }

    // ------------------------------------------------------------------ baca

    @Override
    public Optional<Pemeriksaan> findById(String id) {
        try (Connection c = Jdbc.buka()) {
            return findById(c, id);
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
        }
    }

    /** Dipakai DAO lain agar satu operasi memakai satu koneksi. */
    Optional<Pemeriksaan> findById(Connection c, String id) throws SQLException {
        if (id == null) return Optional.empty();
        try (PreparedStatement ps = c.prepareStatement(SQL_BY_ID)) {
            ps.setString(1, id);
            List<Pemeriksaan> hasil = baca(c, ps);
            return hasil.isEmpty() ? Optional.empty() : Optional.of(hasil.get(0));
        }
    }

    @Override
    public List<Pemeriksaan> findAll() {
        return daftar(SQL_SEMUA, null);
    }

    public List<Pemeriksaan> findByStatus(StatusPemeriksaan status) {
        return daftar(SQL_BY_STATUS, status.name());
    }

    public List<Pemeriksaan> findByDokterId(String idDokter) {
        if (idDokter == null || idDokter.trim().isEmpty()) return new ArrayList<>();
        return daftar(SQL_BY_DOKTER, idDokter.trim());
    }

    public List<Pemeriksaan> findByPasienId(String idPasien) {
        if (idPasien == null || idPasien.trim().isEmpty()) return new ArrayList<>();
        return daftar(SQL_BY_PASIEN, idPasien.trim());
    }

    /** Antrean satu hari, misalnya hari ini. */
    public List<Pemeriksaan> findByTanggal(LocalDate tanggal) {
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(SQL_BY_TANGGAL)) {
            ps.setDate(1, Date.valueOf(tanggal));
            return baca(c, ps);
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
        }
    }

    /** Definisi riwayat (PRD bagian 4): ada baris pemeriksaan yang merujuk dokter ini. */
    public boolean existsByDokterId(String dokterId) {
        return ada(SQL_ADA_DOKTER, dokterId);
    }

    public boolean existsByPasienId(String pasienId) {
        return ada(SQL_ADA_PASIEN, pasienId);
    }

    // ------------------------------------------------------------------ internal

    private boolean ada(String sql, String id) {
        if (id == null || id.trim().isEmpty()) return false;
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
        }
    }

    private List<Pemeriksaan> daftar(String sql, String parameter) {
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (parameter != null) ps.setString(1, parameter);
            return baca(c, ps);
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
        }
    }

    /** Menjalankan query dan menyusun objek. Pasien, dokter, dan layanan dimuat sekali per ID lewat koneksi yang sama. */
    private List<Pemeriksaan> baca(Connection c, PreparedStatement ps) throws SQLException {
        Map<String, Pasien> cachePasien = new HashMap<>();
        Map<String, Dokter> cacheDokter = new HashMap<>();
        Map<String, Layanan> cacheLayanan = new HashMap<>();
        List<Pemeriksaan> hasil = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String idPasien = rs.getString("id_pasien");
                String idDokter = rs.getString("id_dokter");
                String idLayanan = rs.getString("id_layanan");
                if (!cachePasien.containsKey(idPasien)) cachePasien.put(idPasien, pasienDAO.muat(c, idPasien));
                if (!cacheDokter.containsKey(idDokter)) cacheDokter.put(idDokter, dokterDAO.muat(c, idDokter));
                if (!cacheLayanan.containsKey(idLayanan)) cacheLayanan.put(idLayanan, layananDAO.muat(c, idLayanan));
                hasil.add(new Pemeriksaan(
                        rs.getString("id_pemeriksaan"),
                        rs.getInt("no_antrean"),
                        cachePasien.get(idPasien),
                        cacheDokter.get(idDokter),
                        cacheLayanan.get(idLayanan),
                        rs.getString("id_petugas"),
                        rs.getString("nama_petugas"),
                        Jdbc.tanggal(rs, "tanggal_pemeriksaan"),
                        rs.getString("keluhan"),
                        rs.getString("durasi_keluhan_awal"),
                        JenisPenjamin.fromString(rs.getString("jenis_penjamin")),
                        StatusPemeriksaan.fromString(rs.getString("status")),
                        rs.getInt("jumlah_panggilan"),
                        Jdbc.teks(rs, "alasan_batal"),
                        Jdbc.teks(rs, "dibatalkan_oleh")));
            }
        }
        return hasil;
    }
}

package com.klinik.dao;

import com.klinik.exception.DataNotFoundException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.model.JenisSurat;
import com.klinik.model.StatusSurat;
import com.klinik.model.Surat;
import com.klinik.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object untuk entitas Surat (Surat Izin Sakit & Surat Rujukan)
 * menggunakan JDBC PostgreSQL (tabel surat dan view v_surat).
 * Sinkron dengan constraint chk_srt_isian, chk_srt_serah, serta trigger trg_surat_insert dan trg_surat_kunci (BR-17).
 */
public class SuratDAO implements GenericDAO<Surat, String> {

    public SuratDAO() {
    }

    public SuratDAO(String ignoredDataDir) {
    }

    public String generateNextId() {
        return Jdbc.idBerikut("SRT", "seq_surat");
    }

    /**
     * Menyimpan surat baru ke PostgreSQL dengan status awal DITERUSKAN (BR-17).
     */
    public Surat simpan(Surat surat) throws DataTidakValidException {
        if (surat == null) {
            throw new DataTidakValidException("Surat", "Objek surat tidak boleh null.");
        }
        surat.validate();
        if (surat.getTanggalDibuat() == null) {
            surat.setTanggalDibuat(LocalDate.now());
        }
        if (surat.getStatus() != StatusSurat.DISERAHKAN) {
            surat.setStatus(StatusSurat.DITERUSKAN);
        }

        boolean hasId = surat.getIdSurat() != null && !surat.getIdSurat().trim().isEmpty();
        String sql = hasId
                ? "INSERT INTO surat (id_surat, id_pemeriksaan, jenis, isi, jumlah_hari, tujuan_rujukan, tanggal_dibuat, status, diserahkan_oleh) "
                  + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id_surat"
                : "INSERT INTO surat (id_pemeriksaan, jenis, isi, jumlah_hari, tujuan_rujukan, tanggal_dibuat, status, diserahkan_oleh) "
                  + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id_surat";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            if (hasId) {
                ps.setString(idx++, surat.getIdSurat().trim());
            }
            ps.setString(idx++, surat.getIdPemeriksaan().trim());
            ps.setString(idx++, surat.getJenis().name());
            ps.setString(idx++, surat.getIsi());

            // chk_srt_isian: IZIN wajib jumlah_hari >= 1 & tujuan_rujukan NULL; RUJUKAN wajib jumlah_hari NULL & tujuan_rujukan terisi
            if (surat.getJenis() == JenisSurat.IZIN) {
                ps.setInt(idx++, surat.getJumlahHari());
                ps.setNull(idx++, Types.VARCHAR);
            } else {
                ps.setNull(idx++, Types.INTEGER);
                ps.setString(idx++, surat.getTujuanRujukan().trim());
            }

            ps.setDate(idx++, Date.valueOf(surat.getTanggalDibuat()));
            ps.setString(idx++, StatusSurat.DITERUSKAN.name());
            ps.setNull(idx, Types.VARCHAR);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    surat.setIdSurat(rs.getString(1));
                }
            }
            return surat;
        } catch (SQLException e) {
            throw Jdbc.toKlinikException(e, "Surat", surat.getIdSurat());
        }
    }

    @Override
    public void insert(Surat entity) {
        simpan(entity);
    }

    @Override
    public void update(Surat entity) throws DataTidakValidException, DataNotFoundException {
        if (entity == null || entity.getIdSurat() == null) {
            throw new DataTidakValidException("Surat", "ID Surat yang akan diperbarui tidak valid.");
        }
        Surat existing = findById(entity.getIdSurat())
                .orElseThrow(() -> new DataNotFoundException("Surat dengan ID " + entity.getIdSurat() + " tidak ditemukan."));
        if (existing.getStatus() == StatusSurat.DISERAHKAN) {
            throw new DataTidakValidException("Status Surat",
                    "BR-17: Surat " + existing.getIdSurat() + " sudah berstatus DISERAHKAN dan tidak dapat diubah.");
        }
        entity.validate();

        String sql = "UPDATE surat SET id_pemeriksaan = ?, jenis = ?, isi = ?, jumlah_hari = ?, tujuan_rujukan = ?, "
                   + "tanggal_dibuat = ?, status = ?, diserahkan_oleh = ? WHERE id_surat = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entity.getIdPemeriksaan().trim());
            ps.setString(2, entity.getJenis().name());
            ps.setString(3, entity.getIsi());
            if (entity.getJenis() == JenisSurat.IZIN) {
                ps.setInt(4, entity.getJumlahHari());
                ps.setNull(5, Types.VARCHAR);
            } else {
                ps.setNull(4, Types.INTEGER);
                ps.setString(5, entity.getTujuanRujukan().trim());
            }
            ps.setDate(6, Date.valueOf(entity.getTanggalDibuat() != null ? entity.getTanggalDibuat() : LocalDate.now()));
            ps.setString(7, entity.getStatus().name());
            if (entity.getStatus() == StatusSurat.DISERAHKAN) {
                ps.setString(8, Jdbc.emptyToNull(entity.getDiserahkanOleh()));
            } else {
                ps.setNull(8, Types.VARCHAR);
            }
            ps.setString(9, entity.getIdSurat().trim());

            if (ps.executeUpdate() == 0) {
                throw new DataNotFoundException("Surat dengan ID " + entity.getIdSurat() + " tidak ditemukan.");
            }
        } catch (SQLException e) {
            throw Jdbc.toKlinikException(e, "Surat", entity.getIdSurat());
        }
    }

    @Override
    public void delete(String id) throws DataTidakValidException {
        if (id == null || id.trim().isEmpty()) {
            return;
        }
        Optional<Surat> existing = findById(id.trim());
        if (existing.isPresent() && existing.get().getStatus() == StatusSurat.DISERAHKAN) {
            throw new DataTidakValidException("Status Surat",
                    "BR-17: Surat " + id + " sudah berstatus DISERAHKAN dan tidak dapat dihapus.");
        }
        String sql = "DELETE FROM surat WHERE id_surat = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.trim());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw Jdbc.toKlinikException(e, "Surat", id.trim());
        }
    }

    /**
     * FR-30 & BR-17: Mengubah status surat dari DITERUSKAN menjadi DISERAHKAN serta mencatat petugas yang menyerahkan.
     * Surat yang sudah DISERAHKAN terkunci baik di model Java maupun lewat trigger trg_surat_kunci di PostgreSQL.
     */
    public Surat ubahStatusDiserahkan(String idSurat, String diserahkanOleh)
            throws DataNotFoundException, DataTidakValidException {
        if (idSurat == null || idSurat.trim().isEmpty()) {
            throw new DataTidakValidException("ID Surat", "ID Surat wajib dipilih.");
        }
        Surat surat = findById(idSurat.trim())
                .orElseThrow(() -> new DataNotFoundException("Surat dengan ID " + idSurat + " tidak ditemukan."));
        if (surat.getStatus() == StatusSurat.DISERAHKAN) {
            throw new DataTidakValidException("Status Surat",
                    "Surat " + idSurat + " sudah diserahkan sebelumnya oleh " + surat.getDiserahkanOleh() + ".");
        }
        surat.tandaiDiserahkan(diserahkanOleh);

        String sql = "UPDATE surat SET status = 'DISERAHKAN', diserahkan_oleh = ? WHERE id_surat = ? AND status = 'DITERUSKAN'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, surat.getDiserahkanOleh());
            ps.setString(2, surat.getIdSurat());
            if (ps.executeUpdate() != 1) {
                throw new DataTidakValidException("Status Surat", "Surat sudah diserahkan oleh pengguna lain.");
            }
            return surat;
        } catch (SQLException e) {
            throw Jdbc.toKlinikException(e, "Surat", idSurat);
        }
    }

    @Override
    public Optional<Surat> findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return Optional.empty();
        }
        String sql = "SELECT id_surat, id_pemeriksaan, jenis, isi, jumlah_hari, tujuan_rujukan, "
                   + "tanggal_dibuat, status, diserahkan_oleh, id_pasien, nama_pasien, id_dokter, nama_dokter "
                   + "FROM v_surat WHERE id_surat = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw Jdbc.toKlinikException(e, "Surat", id);
        }
        return Optional.empty();
    }

    @Override
    public List<Surat> findAll() {
        return filterSurat(null, null, null);
    }

    public List<Surat> findByStatus(StatusSurat status) {
        return filterSurat(null, status, null);
    }

    public List<Surat> findByJenis(JenisSurat jenis) {
        return filterSurat(jenis, null, null);
    }

    public List<Surat> findByPemeriksaanId(String idPemeriksaan) {
        if (idPemeriksaan == null || idPemeriksaan.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<Surat> list = new ArrayList<>();
        String sql = "SELECT id_surat, id_pemeriksaan, jenis, isi, jumlah_hari, tujuan_rujukan, "
                   + "tanggal_dibuat, status, diserahkan_oleh, id_pasien, nama_pasien, id_dokter, nama_dokter "
                   + "FROM v_surat WHERE lower(id_pemeriksaan) = lower(?) ORDER BY id_surat";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idPemeriksaan.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw Jdbc.toKlinikException(e, "Surat", idPemeriksaan);
        }
        return list;
    }

    /**
     * Memfilter daftar surat menggunakan view v_surat berdasarkan jenis, status, dan kata kunci pencarian.
     */
    public List<Surat> filterSurat(JenisSurat jenis, StatusSurat status, String keyword) {
        StringBuilder sql = new StringBuilder(
                "SELECT id_surat, id_pemeriksaan, jenis, isi, jumlah_hari, tujuan_rujukan, "
                + "tanggal_dibuat, status, diserahkan_oleh, id_pasien, nama_pasien, id_dokter, nama_dokter "
                + "FROM v_surat WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (jenis != null) {
            sql.append(" AND jenis = ?");
            params.add(jenis.name());
        }
        if (status != null) {
            sql.append(" AND status = ?");
            params.add(status.name());
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = "%" + keyword.trim().toLowerCase() + "%";
            sql.append(" AND (lower(id_surat) LIKE ? OR lower(id_pemeriksaan) LIKE ? "
                    + "OR lower(nama_pasien) LIKE ? OR lower(nama_dokter) LIKE ? "
                    + "OR lower(isi) LIKE ? OR lower(COALESCE(tujuan_rujukan, '')) LIKE ? "
                    + "OR lower(COALESCE(diserahkan_oleh, '')) LIKE ?)");
            for (int i = 0; i < 7; i++) {
                params.add(kw);
            }
        }
        sql.append(" ORDER BY id_surat");

        List<Surat> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw Jdbc.toKlinikException(e, "Surat", null);
        }
        return list;
    }

    private Surat mapRow(ResultSet rs) throws SQLException {
        Date tgl = rs.getDate("tanggal_dibuat");
        int jmlHari = rs.getInt("jumlah_hari");
        if (rs.wasNull()) {
            jmlHari = 0;
        }
        return new Surat(
                rs.getString("id_surat"),
                rs.getString("id_pemeriksaan"),
                JenisSurat.fromString(rs.getString("jenis")),
                rs.getString("isi"),
                jmlHari,
                Jdbc.teks(rs, "tujuan_rujukan"),
                tgl != null ? tgl.toLocalDate() : LocalDate.now(),
                StatusSurat.fromString(rs.getString("status")),
                Jdbc.teks(rs, "diserahkan_oleh")
        );
    }
}

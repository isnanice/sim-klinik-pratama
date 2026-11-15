package com.klinik.dao;

import com.klinik.exception.DataNotFoundException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.model.JenisSurat;
import com.klinik.model.StatusSurat;
import com.klinik.model.Surat;

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
 * DAO surat izin dan rujukan di PostgreSQL (tabel surat). Aturan BR-17 dijaga model Surat,
 * SuratController, dan trigger database (surat DISERAHKAN terkunci).
 */
public class SuratDAO implements GenericDAO<Surat, String> {

    private static final String SELECT_DASAR =
            "SELECT id_surat, id_pemeriksaan, jenis, isi, jumlah_hari, tujuan_rujukan, tanggal_dibuat, status, diserahkan_oleh "
          + "FROM surat ";
    private static final String URUTAN = " ORDER BY id_surat";

    static final String SQL_SEMUA        = SELECT_DASAR + URUTAN;
    static final String SQL_BY_ID        = SELECT_DASAR + "WHERE id_surat = ?";
    static final String SQL_BY_STATUS    = SELECT_DASAR + "WHERE status = ?" + URUTAN;
    static final String SQL_BY_JENIS     = SELECT_DASAR + "WHERE jenis = ?" + URUTAN;
    static final String SQL_BY_PEMERIKSAAN = SELECT_DASAR + "WHERE id_pemeriksaan = ?" + URUTAN;
    static final String SQL_FILTER = SELECT_DASAR
            + "WHERE (?::text IS NULL OR jenis = ?::text) AND (?::text IS NULL OR status = ?::text) "
            + "AND (?::text IS NULL OR lower(id_surat) LIKE ?::text OR lower(id_pemeriksaan) LIKE ?::text "
            + "OR lower(isi) LIKE ?::text OR lower(COALESCE(tujuan_rujukan, '')) LIKE ?::text "
            + "OR lower(COALESCE(diserahkan_oleh, '')) LIKE ?::text)" + URUTAN;
    static final String SQL_INSERT =
            "INSERT INTO surat (id_surat, id_pemeriksaan, jenis, isi, jumlah_hari, tujuan_rujukan, tanggal_dibuat, status, diserahkan_oleh) "
          + "VALUES (COALESCE(?, fn_id('SRT', nextval('seq_surat'))), ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id_surat";
    static final String SQL_UPDATE =
            "UPDATE surat SET jenis = ?, isi = ?, jumlah_hari = ?, tujuan_rujukan = ? WHERE id_surat = ?";
    static final String SQL_SERAHKAN =
            "UPDATE surat SET status = 'DISERAHKAN', diserahkan_oleh = ? WHERE id_surat = ? AND status = 'DITERUSKAN'";
    static final String SQL_DELETE = "DELETE FROM surat WHERE id_surat = ?";

    public SuratDAO() { }

    public String generateNextId() {
        return Jdbc.idBerikut("SRT", "seq_surat");
    }

    /** Menyimpan surat baru dengan status awal DITERUSKAN. */
    public Surat simpan(Surat surat) throws DataTidakValidException {
        if (surat == null) {
            throw new DataTidakValidException("Surat", "Objek surat tidak boleh null.");
        }
        surat.validate();
        if (surat.getTanggalDibuat() == null) {
            surat.setTanggalDibuat(LocalDate.now());
        }
        surat.setStatus(StatusSurat.DITERUSKAN);
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(SQL_INSERT)) {
            if (surat.getIdSurat() == null || surat.getIdSurat().trim().isEmpty()) {
                ps.setNull(1, Types.VARCHAR);
            } else {
                ps.setString(1, surat.getIdSurat().trim());
            }
            ps.setString(2, surat.getIdPemeriksaan());
            ps.setString(3, surat.getJenis().name());
            ps.setString(4, surat.getIsi());
            isiIzinRujukan(ps, surat, 5, 6);
            ps.setDate(7, Date.valueOf(surat.getTanggalDibuat()));
            ps.setString(8, StatusSurat.DITERUSKAN.name());
            ps.setNull(9, Types.VARCHAR);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                surat.setIdSurat(rs.getString(1));
            }
            return surat;
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
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
                    "Surat " + existing.getIdSurat() + " sudah berstatus DISERAHKAN dan tidak dapat diubah.");
        }
        entity.validate();
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, entity.getJenis().name());
            ps.setString(2, entity.getIsi());
            isiIzinRujukan(ps, entity, 3, 4);
            ps.setString(5, entity.getIdSurat());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
        }
    }

    /** Izin: jumlah hari terisi dan tujuan NULL. Rujukan: tujuan terisi dan jumlah hari NULL (chk_srt_isian). */
    private void isiIzinRujukan(PreparedStatement ps, Surat s, int idxHari, int idxTujuan) throws SQLException {
        if (s.getJenis() == JenisSurat.IZIN) {
            ps.setInt(idxHari, s.getJumlahHari());
            ps.setNull(idxTujuan, Types.VARCHAR);
        } else {
            ps.setNull(idxHari, Types.INTEGER);
            ps.setString(idxTujuan, s.getTujuanRujukan().trim());
        }
    }

    @Override
    public void delete(String id) throws DataTidakValidException {
        if (id == null) return;
        Optional<Surat> existing = findById(id);
        if (existing.isPresent() && existing.get().getStatus() == StatusSurat.DISERAHKAN) {
            throw new DataTidakValidException("Status Surat",
                    "Surat " + id + " sudah berstatus DISERAHKAN dan tidak dapat dihapus.");
        }
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(SQL_DELETE)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
        }
    }

    /** DITERUSKAN menjadi DISERAHKAN beserta petugas penyerah. Surat DISERAHKAN bersifat final. */
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
        surat.tandaiDiserahkan(diserahkanOleh);          // validasi petugas penyerah
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(SQL_SERAHKAN)) {
            ps.setString(1, surat.getDiserahkanOleh());
            ps.setString(2, surat.getIdSurat());
            if (ps.executeUpdate() != 1) {
                throw new DataTidakValidException("Status Surat", "Surat sudah diserahkan oleh pengguna lain.");
            }
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
        }
        return surat;
    }

    @Override
    public Optional<Surat> findById(String id) {
        if (id == null) return Optional.empty();
        List<Surat> hasil = daftar(SQL_BY_ID, id);
        return hasil.isEmpty() ? Optional.empty() : Optional.of(hasil.get(0));
    }

    @Override
    public List<Surat> findAll() {
        return daftar(SQL_SEMUA);
    }

    public List<Surat> findByStatus(StatusSurat status) {
        return status == null ? findAll() : daftar(SQL_BY_STATUS, status.name());
    }

    public List<Surat> findByJenis(JenisSurat jenis) {
        return jenis == null ? findAll() : daftar(SQL_BY_JENIS, jenis.name());
    }

    public List<Surat> findByPemeriksaanId(String idPemeriksaan) {
        if (idPemeriksaan == null || idPemeriksaan.trim().isEmpty()) return new ArrayList<>();
        return daftar(SQL_BY_PEMERIKSAAN, idPemeriksaan.trim());
    }

    /** Filter berdasarkan jenis, status, dan kata kunci (kosong berarti semua). */
    public List<Surat> filterSurat(JenisSurat jenis, StatusSurat status, String keyword) {
        String j = jenis != null ? jenis.name() : null;
        String s = status != null ? status.name() : null;
        String kw = (keyword == null || keyword.trim().isEmpty()) ? null : Jdbc.like(keyword);
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(SQL_FILTER)) {
            ps.setString(1, j);
            ps.setString(2, j);
            ps.setString(3, s);
            ps.setString(4, s);
            ps.setString(5, kw);
            for (int i = 6; i <= 10; i++) ps.setString(i, kw);
            return baca(ps);
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
        }
    }

    private List<Surat> daftar(String sql, String... parameter) {
        try (Connection c = Jdbc.buka(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < parameter.length; i++) ps.setString(i + 1, parameter[i]);
            return baca(ps);
        } catch (SQLException e) {
            throw Jdbc.gagal(e);
        }
    }

    private List<Surat> baca(PreparedStatement ps) throws SQLException {
        List<Surat> hasil = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int hari = rs.getInt("jumlah_hari");            // NULL (rujukan) terbaca 0
                hasil.add(new Surat(
                        rs.getString("id_surat"),
                        rs.getString("id_pemeriksaan"),
                        JenisSurat.fromString(rs.getString("jenis")),
                        rs.getString("isi"),
                        hari,
                        Jdbc.teks(rs, "tujuan_rujukan"),
                        Jdbc.tanggal(rs, "tanggal_dibuat"),
                        StatusSurat.fromString(rs.getString("status")),
                        Jdbc.teks(rs, "diserahkan_oleh")));
            }
        }
        return hasil;
    }
}

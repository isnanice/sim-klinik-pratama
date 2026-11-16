package com.klinik.dao;

import com.klinik.exception.DataNotFoundException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.model.JenisSurat;
import com.klinik.model.StatusSurat;
import com.klinik.model.Surat;
import com.klinik.util.DatabaseConnection;
import com.klinik.util.PesanDb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Data Access Object untuk entitas Surat (Izin Sakit / Rujukan).
 * Terintegrasi dengan database PostgreSQL (default) serta mendukung mode file untuk pengujian.
 */
public class SuratDAO implements GenericDAO<Surat, String> {
    private final String dataDir;
    private final String filePath;
    private final Map<String, Surat> storage = new LinkedHashMap<>();
    private int counter = 0;

    public SuratDAO() {
        this(null);
    }

    public SuratDAO(String dataDir) {
        this.dataDir = (dataDir != null && !dataDir.trim().isEmpty()) ? dataDir.trim() : null;
        this.filePath = (this.dataDir != null) ? this.dataDir + File.separator + "surat.txt" : null;
        if (isFileMode()) {
            initStorage();
        }
    }

    private boolean isFileMode() {
        return this.filePath != null;
    }

    private void initStorage() {
        File dir = new File(dataDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(filePath);
        if (file.exists()) {
            loadFromFile();
        } else {
            seedInitialData();
            saveToFile();
        }
    }

    private void seedInitialData() {
        Surat s1 = new Surat(
                generateNextId(),
                "PMR-001",
                JenisSurat.IZIN,
                "Pasien mengalami ISPA dan membutuhkan istirahat medis di rumah.",
                3,
                "",
                LocalDate.now(),
                StatusSurat.DITERUSKAN,
                ""
        );
        storage.put(s1.getIdSurat(), s1);

        Surat s2 = new Surat(
                generateNextId(),
                "PMR-002",
                JenisSurat.RUJUKAN,
                "Mohon pemeriksaan endoskopi lanjutan atas indikasi gastritis kronis.",
                0,
                "RSUP Dr. Sardjito - Poli Gastroenterologi",
                LocalDate.now(),
                StatusSurat.DISERAHKAN,
                "Rudi Hartono"
        );
        storage.put(s2.getIdSurat(), s2);
    }

    public synchronized String generateNextId() {
        if (!isFileMode()) {
            String sql = "SELECT fn_id('SRT', nextval('seq_surat'))";
            try (Connection c = DatabaseConnection.getConnection();
                 Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            } catch (Exception ignored) {}
        }
        counter++;
        return String.format("SRT-%03d", counter);
    }

    private void updateCounterFromId(String id) {
        if (id != null && id.startsWith("SRT-")) {
            try {
                int val = Integer.parseInt(id.substring(4));
                if (val > counter) {
                    counter = val;
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    @Override
    public synchronized void insert(Surat s) {
        if (s == null) return;

        if (isFileMode()) {
            if (s.getIdSurat() == null || s.getIdSurat().trim().isEmpty()) {
                s.setIdSurat(generateNextId());
            }
            updateCounterFromId(s.getIdSurat());
            storage.put(s.getIdSurat(), s);
            saveToFile();
            return;
        }

        String sql = "INSERT INTO surat (id_surat, id_pemeriksaan, jenis, isi, jumlah_hari, "
                   + "tujuan_rujukan, tanggal_dibuat, status, diserahkan_oleh) "
                   + "VALUES (COALESCE(?, fn_id('SRT', nextval('seq_surat'))), ?, ?, ?, ?, ?, "
                   + "COALESCE(?, CURRENT_DATE), COALESCE(?, 'DITERUSKAN'), ?) "
                   + "RETURNING id_surat, tanggal_dibuat";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, (s.getIdSurat() != null && !s.getIdSurat().isBlank()) ? s.getIdSurat() : null);
            ps.setString(2, s.getIdPemeriksaan());
            ps.setString(3, s.getJenis() != null ? s.getJenis().name() : "IZIN");
            ps.setString(4, s.getIsi() != null ? s.getIsi() : "Keterangan surat");

            if (s.getJumlahHari() > 0) {
                ps.setInt(5, s.getJumlahHari());
            } else {
                ps.setNull(5, Types.INTEGER);
            }

            ps.setString(6, (s.getTujuanRujukan() != null && !s.getTujuanRujukan().isBlank()) ? s.getTujuanRujukan() : null);
            ps.setDate(7, s.getTanggalDibuat() != null ? Date.valueOf(s.getTanggalDibuat()) : Date.valueOf(LocalDate.now()));
            ps.setString(8, s.getStatus() != null ? s.getStatus().name() : "DITERUSKAN");
            ps.setString(9, (s.getDiserahkanOleh() != null && !s.getDiserahkanOleh().isBlank()) ? s.getDiserahkanOleh() : null);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    s.setIdSurat(rs.getString("id_surat"));
                    Date tgl = rs.getDate("tanggal_dibuat");
                    if (tgl != null) s.setTanggalDibuat(tgl.toLocalDate());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Gagal menambah surat: " + PesanDb.untukPengguna(e), e);
        }
    }

    public synchronized Surat simpan(Surat s) {
        insert(s);
        return s;
    }

    @Override
    public synchronized void update(Surat s) {
        if (s == null || s.getIdSurat() == null) return;

        if (isFileMode()) {
            storage.put(s.getIdSurat(), s);
            saveToFile();
            return;
        }

        String sql = "UPDATE surat SET isi = ?, status = ?, diserahkan_oleh = ? WHERE id_surat = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, s.getIsi());
            ps.setString(2, s.getStatus() != null ? s.getStatus().name() : "DITERUSKAN");
            ps.setString(3, s.getDiserahkanOleh());
            ps.setString(4, s.getIdSurat());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Gagal memperbarui surat: " + PesanDb.untukPengguna(e), e);
        }
    }

    public void ubah(Surat s) {
        update(s);
    }

    @Override
    public synchronized void delete(String id) {
        if (id == null) return;

        if (isFileMode()) {
            storage.remove(id);
            saveToFile();
            return;
        }

        String sql = "DELETE FROM surat WHERE id_surat = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Gagal menghapus surat: " + PesanDb.untukPengguna(e), e);
        }
    }

    public void hapus(String id) {
        delete(id);
    }

    public synchronized Surat ubahStatusDiserahkan(String idSurat, String diserahkanOleh)
            throws DataNotFoundException, DataTidakValidException {
        Surat surat = findById(idSurat)
                .orElseThrow(() -> new DataNotFoundException("Surat dengan ID " + idSurat + " tidak ditemukan."));
        if (surat.getStatus() == StatusSurat.DISERAHKAN) {
            throw new DataTidakValidException("Status Surat", "Surat sudah diserahkan sebelumnya.");
        }
        surat.setStatus(StatusSurat.DISERAHKAN);
        surat.setDiserahkanOleh(diserahkanOleh != null ? diserahkanOleh : "Petugas");
        update(surat);
        return surat;
    }

    @Override
    public synchronized Optional<Surat> findById(String id) {
        if (id == null) return Optional.empty();

        if (isFileMode()) {
            return Optional.ofNullable(storage.get(id));
        }

        String sql = "SELECT * FROM surat WHERE id_surat = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSurat(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal mencari surat by ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    public synchronized List<Surat> findByIdPemeriksaan(String idPemeriksaan) {
        return findByPemeriksaanId(idPemeriksaan);
    }

    public synchronized List<Surat> findByPemeriksaanId(String idPemeriksaan) {
        if (idPemeriksaan == null || idPemeriksaan.trim().isEmpty()) {
            return new ArrayList<>();
        }

        if (isFileMode()) {
            String clean = idPemeriksaan.trim();
            return storage.values().stream()
                    .filter(s -> s.getIdPemeriksaan() != null && s.getIdPemeriksaan().equalsIgnoreCase(clean))
                    .collect(Collectors.toList());
        }

        List<Surat> list = new ArrayList<>();
        String sql = "SELECT * FROM surat WHERE id_pemeriksaan = ? ORDER BY id_surat";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, idPemeriksaan.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSurat(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal memuat surat by pemeriksaan: " + e.getMessage());
        }
        return list;
    }

    @Override
    public synchronized List<Surat> findAll() {
        if (isFileMode()) {
            return new ArrayList<>(storage.values());
        }

        List<Surat> list = new ArrayList<>();
        String sql = "SELECT * FROM surat ORDER BY id_surat DESC";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToSurat(rs));
            }
        } catch (SQLException e) {
            System.err.println("Gagal memuat semua surat: " + e.getMessage());
        }
        return list;
    }

    public synchronized List<Surat> findByStatus(StatusSurat status) {
        if (status == null) {
            return findAll();
        }
        return findAll().stream()
                .filter(s -> s.getStatus() == status)
                .collect(Collectors.toList());
    }

    public synchronized List<Surat> findByJenis(JenisSurat jenis) {
        if (jenis == null) {
            return findAll();
        }
        return findAll().stream()
                .filter(s -> s.getJenis() == jenis)
                .collect(Collectors.toList());
    }

    public synchronized List<Surat> filterSurat(JenisSurat jenis, StatusSurat status, String keyword) {
        String kw = keyword != null ? keyword.trim().toLowerCase() : "";
        return findAll().stream()
                .filter(s -> jenis == null || s.getJenis() == jenis)
                .filter(s -> status == null || s.getStatus() == status)
                .filter(s -> {
                    if (kw.isEmpty()) return true;
                    return (s.getIdSurat() != null && s.getIdSurat().toLowerCase().contains(kw)) ||
                           (s.getIdPemeriksaan() != null && s.getIdPemeriksaan().toLowerCase().contains(kw)) ||
                           (s.getIsi() != null && s.getIsi().toLowerCase().contains(kw)) ||
                           (s.getTujuanRujukan() != null && s.getTujuanRujukan().toLowerCase().contains(kw)) ||
                           (s.getDiserahkanOleh() != null && s.getDiserahkanOleh().toLowerCase().contains(kw));
                })
                .collect(Collectors.toList());
    }

    private Surat mapResultSetToSurat(ResultSet rs) throws SQLException {
        String id = rs.getString("id_surat");
        String idPemeriksaan = rs.getString("id_pemeriksaan");
        String jenisStr = rs.getString("jenis");
        String isi = rs.getString("isi");
        int jmlHari = rs.getInt("jumlah_hari");
        String tujuan = rs.getString("tujuan_rujukan");
        Date tgl = rs.getDate("tanggal_dibuat");
        String statusStr = rs.getString("status");
        String diserahkanOleh = rs.getString("diserahkan_oleh");

        JenisSurat jenis = JenisSurat.fromString(jenisStr);
        StatusSurat status = StatusSurat.fromString(statusStr);
        LocalDate tglDibuat = tgl != null ? tgl.toLocalDate() : LocalDate.now();

        try {
            return new Surat(id, idPemeriksaan, jenis, isi, jmlHari, tujuan, tglDibuat, status, diserahkanOleh);
        } catch (Exception e) {
            Surat s = new Surat();
            s.setIdSurat(id);
            s.setIdPemeriksaan(idPemeriksaan);
            return s;
        }
    }

    private synchronized void saveToFile() {
        if (filePath == null) return;
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            for (Surat s : storage.values()) {
                String line = String.join("|",
                        escape(s.getIdSurat()),
                        escape(s.getIdPemeriksaan()),
                        s.getJenis() != null ? s.getJenis().name() : "",
                        escape(s.getIsi()),
                        String.valueOf(s.getJumlahHari()),
                        escape(s.getTujuanRujukan()),
                        s.getTanggalDibuat() != null ? s.getTanggalDibuat().toString() : "",
                        s.getStatus() != null ? s.getStatus().name() : StatusSurat.DITERUSKAN.name(),
                        escape(s.getDiserahkanOleh())
                );
                writer.println(line);
            }
        } catch (IOException e) {
            System.err.println("Gagal menyimpan surat ke file: " + e.getMessage());
        }
    }

    private synchronized void loadFromFile() {
        if (filePath == null) return;
        storage.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length >= 9) {
                    String idSurat = unescape(parts[0]);
                    String idPemeriksaan = unescape(parts[1]);
                    JenisSurat jenis = JenisSurat.fromString(parts[2]);
                    String isi = unescape(parts[3]);
                    int jumlahHari = 0;
                    try {
                        jumlahHari = Integer.parseInt(parts[4].trim());
                    } catch (NumberFormatException ignored) {}
                    String tujuanRujukan = unescape(parts[5]);
                    LocalDate tglDibuat = parts[6].isEmpty() ? LocalDate.now() : LocalDate.parse(parts[6]);
                    StatusSurat status = StatusSurat.fromString(parts[7]);
                    String diserahkanOleh = unescape(parts[8]);

                    try {
                        Surat s = new Surat(idSurat, idPemeriksaan, jenis, isi, jumlahHari, tujuanRujukan, tglDibuat, status, diserahkanOleh);
                        storage.put(idSurat, s);
                        updateCounterFromId(idSurat);
                    } catch (Exception ignored) {}
                }
            }
        } catch (IOException e) {
            System.err.println("Gagal memuat data surat dari file: " + e.getMessage());
        }
    }

    private String escape(String val) {
        if (val == null) return "";
        return val.replace("\\", "\\\\").replace("|", "\\p").replace("\n", "\\n");
    }

    private String unescape(String val) {
        if (val == null) return "";
        return val.replace("\\n", "\n").replace("\\p", "|").replace("\\\\", "\\");
    }
}

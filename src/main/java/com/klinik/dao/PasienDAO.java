package com.klinik.dao;

import com.klinik.exception.DataMasihDipakaiException;
import com.klinik.exception.DataNotFoundException;
import com.klinik.model.JenisKelamin;
import com.klinik.model.Pasien;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Data Access Object untuk entitas Pasien.
 * Terintegrasi dengan basis data PostgreSQL (default) dan mendukung mode file untuk pengujian.
 */
public class PasienDAO implements GenericDAO<Pasien, String> {
    private final String dataDir;
    private final String filePath;
    private final String pemeriksaanFilePath;
    private final Map<String, Pasien> storage = new LinkedHashMap<>();
    private PemeriksaanDAO pemeriksaanDAO;
    private int counter = 0;

    public PasienDAO() {
        this(null, null);
    }

    public PasienDAO(String dataDir) {
        this(null, dataDir);
    }

    public PasienDAO(PemeriksaanDAO pemeriksaanDAO) {
        this(pemeriksaanDAO, null);
    }

    public PasienDAO(PemeriksaanDAO pemeriksaanDAO, String dataDir) {
        this.pemeriksaanDAO = pemeriksaanDAO;
        this.dataDir = (dataDir != null && !dataDir.trim().isEmpty()) ? dataDir.trim() : null;
        this.filePath = (this.dataDir != null) ? this.dataDir + File.separator + "pasien.txt" : null;
        this.pemeriksaanFilePath = (this.dataDir != null) ? this.dataDir + File.separator + "pemeriksaan.txt" : null;
        if (isFileMode()) {
            initStorage();
        }
    }

    private boolean isFileMode() {
        return this.filePath != null;
    }

    public void setPemeriksaanDAO(PemeriksaanDAO pemeriksaanDAO) {
        this.pemeriksaanDAO = pemeriksaanDAO;
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
        insert(new Pasien(generateNextId(), "3404011205900001", "Budi Santoso", JenisKelamin.LAKI_LAKI, LocalDate.of(1990, 5, 12),
                "Jl. Malioboro No. 12, Yogyakarta", "081234567890", "0001234567890", LocalDate.now(), true));
        insert(new Pasien(generateNextId(), "3404024309850002", "Sari Dewi", JenisKelamin.PEREMPUAN, LocalDate.of(1985, 9, 3),
                "Jl. Gejayan No. 44, Sleman", "081298765432", "0001234567891", LocalDate.now(), true));
        insert(new Pasien(generateNextId(), "3404032101010003", "Agus Prasetyo", JenisKelamin.LAKI_LAKI, LocalDate.of(2001, 1, 21),
                "Jl. Palagan No. 8, Sleman", "085711223344", "0001234567892", LocalDate.now(), true));
        insert(new Pasien(generateNextId(), "3471045407780004", "Rina Wulandari", JenisKelamin.PEREMPUAN, LocalDate.of(1978, 7, 14),
                "Jl. Kusumanegara No. 99, Yogyakarta", "082133445566", "0001234567893", LocalDate.now(), false));
        insert(new Pasien(generateNextId(), "3402053011950005", "Dimas Aditya", JenisKelamin.LAKI_LAKI, LocalDate.of(1995, 11, 30),
                "Jl. Wonosari Km 4, Bantul", "087855667788", "0001234567894", LocalDate.now(), true));
    }

    public synchronized String generateNextId() {
        if (!isFileMode()) {
            String sql = "SELECT fn_id('PSN', nextval('seq_pasien'))";
            try (Connection c = DatabaseConnection.getConnection();
                 Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            } catch (Exception ignored) {}
        }
        counter++;
        return String.format("PSN-%03d", counter);
    }

    private void updateCounterFromId(String id) {
        if (id != null && id.startsWith("PSN-")) {
            try {
                int val = Integer.parseInt(id.substring(4));
                if (val > counter) {
                    counter = val;
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    @Override
    public synchronized void insert(Pasien p) {
        if (p == null) return;

        if (isFileMode()) {
            if (p.getId() == null || p.getId().trim().isEmpty()) {
                p.setId(generateNextId());
            }
            updateCounterFromId(p.getId());
            storage.put(p.getId(), p);
            saveToFile();
            return;
        }

        String sql = "INSERT INTO pasien (id_pasien, nik, no_bpjs, nama, jenis_kelamin, tanggal_lahir, alamat, no_telepon, tanggal_registrasi, status_aktif) "
                   + "VALUES (COALESCE(?, fn_id('PSN', nextval('seq_pasien'))), ?, ?, ?, ?, ?, ?, ?, COALESCE(?, CURRENT_DATE), ?) "
                   + "RETURNING id_pasien, tanggal_registrasi";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            String nik = p.getNik();
            if (nik == null || nik.trim().isEmpty()) {
                nik = String.format("327301%010d", Math.abs(System.nanoTime() % 10000000000L));
                p.setNik(nik);
            }

            ps.setString(1, (p.getId() != null && !p.getId().isBlank()) ? p.getId() : null);
            ps.setString(2, nik);
            ps.setString(3, (p.getNoBpjs() != null && !p.getNoBpjs().isBlank()) ? p.getNoBpjs().trim() : null);
            ps.setString(4, p.getNama());
            ps.setString(5, p.getJenisKelamin() != null ? p.getJenisKelamin().getLabel() : "Laki-laki");
            ps.setDate(6, p.getTanggalLahir() != null ? Date.valueOf(p.getTanggalLahir()) : Date.valueOf(LocalDate.of(2000, 1, 1)));
            ps.setString(7, p.getAlamat());
            ps.setString(8, p.getNoTelepon());
            ps.setDate(9, p.getTanggalRegistrasi() != null ? Date.valueOf(p.getTanggalRegistrasi()) : Date.valueOf(LocalDate.now()));
            ps.setBoolean(10, p.isAktif());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    p.setId(rs.getString("id_pasien"));
                    Date tglReg = rs.getDate("tanggal_registrasi");
                    if (tglReg != null) {
                        p.setTanggalRegistrasi(tglReg.toLocalDate());
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Gagal menambah data pasien: " + PesanDb.untukPengguna(e), e);
        }
    }

    public void simpan(Pasien p) {
        insert(p);
    }

    @Override
    public synchronized void update(Pasien p) {
        if (p == null || p.getId() == null) return;

        if (isFileMode()) {
            storage.put(p.getId(), p);
            saveToFile();
            return;
        }

        String sql = "UPDATE pasien SET nik = ?, no_bpjs = ?, nama = ?, jenis_kelamin = ?, tanggal_lahir = ?, "
                   + "alamat = ?, no_telepon = ?, status_aktif = ? WHERE id_pasien = ?";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            String nik = p.getNik();
            if (nik == null || nik.trim().isEmpty()) {
                nik = String.format("327301%010d", Math.abs(System.nanoTime() % 10000000000L));
                p.setNik(nik);
            }

            ps.setString(1, nik);
            ps.setString(2, (p.getNoBpjs() != null && !p.getNoBpjs().isBlank()) ? p.getNoBpjs().trim() : null);
            ps.setString(3, p.getNama());
            ps.setString(4, p.getJenisKelamin() != null ? p.getJenisKelamin().getLabel() : "Laki-laki");
            ps.setDate(5, p.getTanggalLahir() != null ? Date.valueOf(p.getTanggalLahir()) : Date.valueOf(LocalDate.of(2000, 1, 1)));
            ps.setString(6, p.getAlamat());
            ps.setString(7, p.getNoTelepon());
            ps.setBoolean(8, p.isAktif());
            ps.setString(9, p.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Gagal memperbarui data pasien: " + PesanDb.untukPengguna(e), e);
        }
    }

    public void ubah(Pasien p) {
        update(p);
    }

    @Override
    public synchronized void delete(String id) throws DataMasihDipakaiException {
        if (id == null) return;

        if (hasRiwayatPemeriksaan(id)) {
            Optional<Pasien> p = findById(id);
            String nama = p.map(Pasien::getNama).orElse(id);
            throw new DataMasihDipakaiException("Pasien", id,
                    "Pasien " + nama + " (" + id + ") memiliki riwayat pemeriksaan dan tidak dapat dihapus permanen. Gunakan fitur Nonaktifkan.");
        }

        if (isFileMode()) {
            storage.remove(id);
            saveToFile();
            return;
        }

        String sql = "DELETE FROM pasien WHERE id_pasien = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (PesanDb.dataDipakai(e)) {
                throw new DataMasihDipakaiException("Pasien", id, PesanDb.untukPengguna(e));
            }
            throw new RuntimeException("Gagal menghapus data pasien: " + PesanDb.untukPengguna(e), e);
        }
    }

    public void hapus(String id) throws DataMasihDipakaiException {
        delete(id);
    }

    public boolean hasRiwayatPemeriksaan(String pasienId) {
        if (pasienId == null || pasienId.trim().isEmpty()) return false;
        if (pemeriksaanDAO != null && pemeriksaanDAO.existsByPasienId(pasienId)) {
            return true;
        }

        if (isFileMode()) {
            if (pemeriksaanFilePath == null) return false;
            File file = new File(pemeriksaanFilePath);
            if (!file.exists()) {
                return false;
            }
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    String[] parts = line.split("\\|", -1);
                    if (parts.length >= 3 && parts[2].equalsIgnoreCase(pasienId.trim())) {
                        return true;
                    }
                }
            } catch (IOException ignored) {}
            return false;
        }

        String sql = "SELECT 1 FROM pemeriksaan WHERE id_pasien = ? LIMIT 1";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, pasienId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ignored) {
            return false;
        }
    }

    public synchronized void toggleAktif(String id, boolean aktif) {
        if (id == null) return;

        if (isFileMode()) {
            Pasien p = storage.get(id);
            if (p != null) {
                p.setAktif(aktif);
                saveToFile();
            }
            return;
        }

        String sql = "UPDATE pasien SET status_aktif = ? WHERE id_pasien = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, aktif);
            ps.setString(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Gagal mengubah status aktif pasien: " + PesanDb.untukPengguna(e), e);
        }
    }

    public void nonaktifkanPasien(String id) throws DataNotFoundException {
        if (findById(id).isEmpty()) {
            throw new DataNotFoundException("Pasien dengan ID " + id + " tidak ditemukan.");
        }
        toggleAktif(id, false);
    }

    @Override
    public synchronized Optional<Pasien> findById(String id) {
        if (id == null) return Optional.empty();

        if (isFileMode()) {
            return Optional.ofNullable(storage.get(id));
        }

        String sql = "SELECT * FROM pasien WHERE id_pasien = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPasien(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal mencari pasien by ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    public synchronized Optional<Pasien> findByNik(String nik) {
        if (nik == null || nik.trim().isEmpty()) return Optional.empty();

        if (isFileMode()) {
            String clean = nik.trim();
            return storage.values().stream()
                    .filter(p -> p.getNik() != null && p.getNik().equalsIgnoreCase(clean))
                    .findFirst();
        }

        String sql = "SELECT * FROM pasien WHERE nik = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nik.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPasien(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal mencari pasien by NIK: " + e.getMessage());
        }
        return Optional.empty();
    }

    public synchronized List<Pasien> findByNama(String nama) {
        if (nama == null || nama.trim().isEmpty()) return findAll();

        if (isFileMode()) {
            String clean = nama.trim().toLowerCase();
            return storage.values().stream()
                    .filter(p -> p.getNama() != null && p.getNama().toLowerCase().contains(clean))
                    .collect(Collectors.toList());
        }

        String sql = "SELECT * FROM pasien WHERE LOWER(nama) LIKE ? ORDER BY id_pasien";
        List<Pasien> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + nama.trim().toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPasien(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal mencari pasien by Nama: " + e.getMessage());
        }
        return list;
    }

    @Override
    public synchronized List<Pasien> findAll() {
        if (isFileMode()) {
            return new ArrayList<>(storage.values());
        }

        List<Pasien> list = new ArrayList<>();
        String sql = "SELECT * FROM pasien ORDER BY id_pasien";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToPasien(rs));
            }
        } catch (SQLException e) {
            System.err.println("Gagal memuat semua pasien: " + e.getMessage());
        }
        return list;
    }

    public synchronized List<Pasien> findActive() {
        if (isFileMode()) {
            return storage.values().stream()
                    .filter(Pasien::isAktif)
                    .collect(Collectors.toList());
        }

        List<Pasien> list = new ArrayList<>();
        String sql = "SELECT * FROM pasien WHERE status_aktif = TRUE ORDER BY id_pasien";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToPasien(rs));
            }
        } catch (SQLException e) {
            System.err.println("Gagal memuat pasien aktif: " + e.getMessage());
        }
        return list;
    }

    public synchronized List<Pasien> search(String keyword, String criteria) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }

        if (isFileMode()) {
            String q = keyword.trim().toLowerCase();
            String krit = criteria == null ? "Semua" : criteria.trim();
            return storage.values().stream()
                    .filter(p -> {
                        if ("ID".equalsIgnoreCase(krit)) {
                            return p.getId() != null && p.getId().toLowerCase().contains(q);
                        } else if ("NIK".equalsIgnoreCase(krit)) {
                            return p.getNik() != null && p.getNik().toLowerCase().contains(q);
                        } else if ("Nama".equalsIgnoreCase(krit)) {
                            return p.getNama() != null && p.getNama().toLowerCase().contains(q);
                        } else if ("No. BPJS".equalsIgnoreCase(krit) || "BPJS".equalsIgnoreCase(krit)) {
                            return p.getNoBpjs() != null && p.getNoBpjs().toLowerCase().contains(q);
                        } else {
                            return (p.getId() != null && p.getId().toLowerCase().contains(q)) ||
                                   (p.getNik() != null && p.getNik().toLowerCase().contains(q)) ||
                                   (p.getNama() != null && p.getNama().toLowerCase().contains(q)) ||
                                   (p.getNoBpjs() != null && p.getNoBpjs().toLowerCase().contains(q));
                        }
                    })
                    .collect(Collectors.toList());
        }

        String q = "%" + keyword.trim().toLowerCase() + "%";
        String sql;
        String krit = criteria == null ? "Semua" : criteria.trim();

        if ("ID".equalsIgnoreCase(krit)) {
            sql = "SELECT * FROM pasien WHERE LOWER(id_pasien) LIKE ? ORDER BY id_pasien";
        } else if ("NIK".equalsIgnoreCase(krit)) {
            sql = "SELECT * FROM pasien WHERE LOWER(nik) LIKE ? ORDER BY id_pasien";
        } else if ("Nama".equalsIgnoreCase(krit)) {
            sql = "SELECT * FROM pasien WHERE LOWER(nama) LIKE ? ORDER BY id_pasien";
        } else if ("No. BPJS".equalsIgnoreCase(krit) || "BPJS".equalsIgnoreCase(krit)) {
            sql = "SELECT * FROM pasien WHERE LOWER(no_bpjs) LIKE ? ORDER BY id_pasien";
        } else {
            sql = "SELECT * FROM pasien WHERE LOWER(id_pasien) LIKE ? OR LOWER(nik) LIKE ? OR LOWER(nama) LIKE ? OR LOWER(no_bpjs) LIKE ? ORDER BY id_pasien";
        }

        List<Pasien> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, q);
            if ("Semua".equalsIgnoreCase(krit)) {
                ps.setString(2, q);
                ps.setString(3, q);
                ps.setString(4, q);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPasien(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal mencari pasien: " + e.getMessage());
        }
        return list;
    }

    public List<Pasien> cari(String keyword) {
        return search(keyword, "Semua");
    }

    private Pasien mapResultSetToPasien(ResultSet rs) throws SQLException {
        String id = rs.getString("id_pasien");
        String nik = rs.getString("nik");
        String noBpjs = rs.getString("no_bpjs");
        String nama = rs.getString("nama");
        String jkStr = rs.getString("jenis_kelamin");
        JenisKelamin jk = JenisKelamin.fromString(jkStr);
        Date tglLahir = rs.getDate("tanggal_lahir");
        String alamat = rs.getString("alamat");
        String noTelp = rs.getString("no_telepon");
        Date tglReg = rs.getDate("tanggal_registrasi");
        boolean aktif = rs.getBoolean("status_aktif");

        LocalDate localLahir = tglLahir != null ? tglLahir.toLocalDate() : null;
        LocalDate localReg = tglReg != null ? tglReg.toLocalDate() : LocalDate.now();

        return new Pasien(id, nik, nama, jk, localLahir, alamat, noTelp, noBpjs, localReg, aktif);
    }

    private synchronized void saveToFile() {
        if (filePath == null) return;
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            for (Pasien p : storage.values()) {
                String line = String.join("|",
                        escape(p.getId()),
                        escape(p.getNik()),
                        escape(p.getNama()),
                        p.getJenisKelamin() != null ? p.getJenisKelamin().name() : "",
                        p.getTanggalLahir() != null ? p.getTanggalLahir().toString() : "",
                        escape(p.getAlamat()),
                        escape(p.getNoTelepon()),
                        escape(p.getNoBpjs()),
                        p.getTanggalRegistrasi() != null ? p.getTanggalRegistrasi().toString() : "",
                        String.valueOf(p.isAktif())
                );
                writer.println(line);
            }
        } catch (IOException e) {
            System.err.println("Gagal menyimpan pasien ke file: " + e.getMessage());
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
                if (parts.length >= 10) {
                    String id = unescape(parts[0]);
                    String nik = unescape(parts[1]);
                    String nama = unescape(parts[2]);
                    JenisKelamin jk = JenisKelamin.fromString(parts[3]);
                    LocalDate tglLahir = parts[4].isEmpty() ? null : LocalDate.parse(parts[4]);
                    String alamat = unescape(parts[5]);
                    String noTelp = unescape(parts[6]);
                    String noBpjs = unescape(parts[7]);
                    LocalDate tglReg = parts[8].isEmpty() ? LocalDate.now() : LocalDate.parse(parts[8]);
                    boolean aktif = Boolean.parseBoolean(parts[9]);

                    Pasien p = new Pasien(id, nik, nama, jk, tglLahir, alamat, noTelp, noBpjs, tglReg, aktif);
                    storage.put(id, p);
                    updateCounterFromId(id);
                }
            }
        } catch (IOException e) {
            System.err.println("Gagal memuat pasien dari file: " + e.getMessage());
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

package com.klinik.dao;

import com.klinik.exception.DataMasihDipakaiException;
import com.klinik.model.Dokter;
import com.klinik.model.JenisKelamin;
import com.klinik.util.DatabaseConnection;
import com.klinik.util.PesanDb;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data Access Object untuk entitas Dokter.
 * Terintegrasi dengan database PostgreSQL (default), serta mendukung direktori kustom untuk pengujian.
 */
public class DokterDAO implements GenericDAO<Dokter, String> {
    private final String dataDir;
    private final String filePath;
    private final Map<String, Dokter> fileStorage = new LinkedHashMap<>();
    private int counter = 0;

    public DokterDAO() {
        this.dataDir = null;
        this.filePath = null;
    }

    public DokterDAO(String dataDir) {
        this.dataDir = (dataDir != null && !dataDir.trim().isEmpty()) ? dataDir.trim() : null;
        this.filePath = (this.dataDir != null) ? this.dataDir + File.separator + "dokter.txt" : null;
        if (this.filePath != null) {
            initFileStorage();
        }
    }

    private boolean isFileMode() {
        return this.filePath != null;
    }

    private void initFileStorage() {
        File dir = new File(dataDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(filePath);
        if (file.exists()) {
            loadFromFile();
        } else {
            seedInitialFileData();
            saveToFile();
        }
    }

    private void seedInitialFileData() {
        insert(new Dokter(generateNextId(), "dr. Ani Wijaya", JenisKelamin.PEREMPUAN,
                "Jl. Malioboro No. 10, Yogyakarta", "081234567890", "STR-11223344", "Umum", true));
        insert(new Dokter(generateNextId(), "dr. Hadi Nugroho, Sp.PD", JenisKelamin.LAKI_LAKI,
                "Jl. Kaliurang Km 5, Sleman", "081298765432", "STR-22334455", "Penyakit Dalam", true));
        insert(new Dokter(generateNextId(), "drg. Maya Indah", JenisKelamin.PEREMPUAN,
                "Jl. Solo No. 45, Yogyakarta", "085711223344", "STR-33445566", "Gigi", true));
        insert(new Dokter(generateNextId(), "dr. Budi Santoso, Sp.A", JenisKelamin.LAKI_LAKI,
                "Jl. Gejayan No. 20, Sleman", "087855667788", "STR-44556677", "Anak", true));
        insert(new Dokter(generateNextId(), "dr. Eko Prasetyo", JenisKelamin.LAKI_LAKI,
                "Jl. Bantul Km 7, Bantul", "082133445566", "STR-55667788", "Umum", false));
    }

    public synchronized String generateNextId() {
        if (!isFileMode()) {
            String sql = "SELECT fn_id('DKT', nextval('seq_dokter'))";
            try (Connection c = DatabaseConnection.getConnection();
                 Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            } catch (Exception ignored) {}
        }
        counter++;
        return String.format("DKT-%03d", counter);
    }

    private void updateCounterFromId(String id) {
        if (id != null && id.startsWith("DKT-")) {
            try {
                int val = Integer.parseInt(id.substring(4));
                if (val > counter) {
                    counter = val;
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    @Override
    public synchronized void insert(Dokter d) {
        if (d == null) return;

        if (isFileMode()) {
            if (d.getId() == null || d.getId().trim().isEmpty()) {
                d.setId(generateNextId());
            } else {
                updateCounterFromId(d.getId());
            }
            fileStorage.put(d.getId(), d);
            saveToFile();
            return;
        }

        String sql = "INSERT INTO dokter (id_dokter, nama, jenis_kelamin, alamat, no_telepon, nomor_registrasi, bidang_pelayanan, status_aktif) "
                   + "VALUES (COALESCE(?, fn_id('DKT', nextval('seq_dokter'))), ?, ?, ?, ?, ?, ?, ?) "
                   + "RETURNING id_dokter";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, (d.getId() != null && !d.getId().isBlank()) ? d.getId() : null);
            ps.setString(2, d.getNama());
            ps.setString(3, d.getJenisKelamin() != null ? d.getJenisKelamin().getLabel() : "Laki-laki");
            ps.setString(4, d.getAlamat());
            ps.setString(5, d.getNoTelepon());
            ps.setString(6, d.getNomorRegistrasi());
            ps.setString(7, d.getBidangPelayanan());
            ps.setBoolean(8, d.isAktif());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    d.setId(rs.getString("id_dokter"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Gagal menambah data dokter: " + PesanDb.untukPengguna(e), e);
        }
    }

    @Override
    public synchronized void update(Dokter d) {
        if (d == null || d.getId() == null) return;

        if (isFileMode()) {
            fileStorage.put(d.getId(), d);
            saveToFile();
            return;
        }

        String sql = "UPDATE dokter SET nama = ?, jenis_kelamin = ?, alamat = ?, no_telepon = ?, "
                   + "nomor_registrasi = ?, bidang_pelayanan = ?, status_aktif = ? WHERE id_dokter = ?";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, d.getNama());
            ps.setString(2, d.getJenisKelamin() != null ? d.getJenisKelamin().getLabel() : "Laki-laki");
            ps.setString(3, d.getAlamat());
            ps.setString(4, d.getNoTelepon());
            ps.setString(5, d.getNomorRegistrasi());
            ps.setString(6, d.getBidangPelayanan());
            ps.setBoolean(7, d.isAktif());
            ps.setString(8, d.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Gagal memperbarui data dokter: " + PesanDb.untukPengguna(e), e);
        }
    }

    @Override
    public synchronized void delete(String id) throws DataMasihDipakaiException {
        if (id == null) return;

        if (isFileMode()) {
            fileStorage.remove(id);
            saveToFile();
            return;
        }

        String sql = "DELETE FROM dokter WHERE id_dokter = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (PesanDb.dataDipakai(e)) {
                throw new DataMasihDipakaiException("Dokter", id, PesanDb.untukPengguna(e));
            }
            throw new RuntimeException("Gagal menghapus data dokter: " + PesanDb.untukPengguna(e), e);
        }
    }

    public synchronized void toggleAktif(String id, boolean aktif) {
        if (isFileMode()) {
            Dokter d = fileStorage.get(id);
            if (d != null) {
                d.setAktif(aktif);
                saveToFile();
            }
            return;
        }

        String sql = "UPDATE dokter SET status_aktif = ? WHERE id_dokter = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, aktif);
            ps.setString(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Gagal mengubah status aktif dokter: " + PesanDb.untukPengguna(e), e);
        }
    }

    @Override
    public synchronized Optional<Dokter> findById(String id) {
        if (id == null) return Optional.empty();

        if (isFileMode()) {
            return Optional.ofNullable(fileStorage.get(id));
        }

        String sql = "SELECT * FROM dokter WHERE id_dokter = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToDokter(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal mencari dokter by ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public synchronized List<Dokter> findAll() {
        if (isFileMode()) {
            return new ArrayList<>(fileStorage.values());
        }

        List<Dokter> list = new ArrayList<>();
        String sql = "SELECT * FROM dokter ORDER BY id_dokter";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToDokter(rs));
            }
        } catch (SQLException e) {
            System.err.println("Gagal memuat semua dokter: " + e.getMessage());
        }
        return list;
    }

    public synchronized List<Dokter> findActive() {
        if (isFileMode()) {
            return fileStorage.values().stream().filter(Dokter::isAktif).collect(Collectors.toList());
        }

        List<Dokter> list = new ArrayList<>();
        String sql = "SELECT * FROM dokter WHERE status_aktif = TRUE ORDER BY id_dokter";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToDokter(rs));
            }
        } catch (SQLException e) {
            System.err.println("Gagal memuat dokter aktif: " + e.getMessage());
        }
        return list;
    }

    public synchronized List<Dokter> search(String keyword, String criteria) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }

        if (isFileMode()) {
            String q = keyword.trim().toLowerCase();
            return fileStorage.values().stream()
                    .filter(d -> {
                        if ("Bidang Pelayanan".equalsIgnoreCase(criteria)) {
                            return d.getBidangPelayanan() != null && d.getBidangPelayanan().toLowerCase().contains(q);
                        } else if ("Nama".equalsIgnoreCase(criteria)) {
                            return d.getNama() != null && d.getNama().toLowerCase().contains(q);
                        } else {
                            return (d.getNama() != null && d.getNama().toLowerCase().contains(q)) ||
                                   (d.getId() != null && d.getId().toLowerCase().contains(q)) ||
                                   (d.getBidangPelayanan() != null && d.getBidangPelayanan().toLowerCase().contains(q));
                        }
                    })
                    .collect(Collectors.toList());
        }

        String q = "%" + keyword.trim().toLowerCase() + "%";
        String sql;
        if ("Nama".equalsIgnoreCase(criteria)) {
            sql = "SELECT * FROM dokter WHERE LOWER(nama) LIKE ? ORDER BY id_dokter";
        } else if ("Bidang Pelayanan".equalsIgnoreCase(criteria)) {
            sql = "SELECT * FROM dokter WHERE LOWER(bidang_pelayanan) LIKE ? ORDER BY id_dokter";
        } else {
            sql = "SELECT * FROM dokter WHERE LOWER(nama) LIKE ? OR LOWER(bidang_pelayanan) LIKE ? OR LOWER(id_dokter) LIKE ? ORDER BY id_dokter";
        }

        List<Dokter> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, q);
            if (!"Nama".equalsIgnoreCase(criteria) && !"Bidang Pelayanan".equalsIgnoreCase(criteria)) {
                ps.setString(2, q);
                ps.setString(3, q);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToDokter(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal mencari dokter: " + e.getMessage());
        }
        return list;
    }

    private Dokter mapResultSetToDokter(ResultSet rs) throws SQLException {
        String id = rs.getString("id_dokter");
        String nama = rs.getString("nama");
        String jkStr = rs.getString("jenis_kelamin");
        JenisKelamin jk = JenisKelamin.fromString(jkStr);
        String alamat = rs.getString("alamat");
        String noTelp = rs.getString("no_telepon");
        String noReg = rs.getString("nomor_registrasi");
        String bidang = rs.getString("bidang_pelayanan");
        boolean aktif = rs.getBoolean("status_aktif");

        return new Dokter(id, nama, jk, alamat, noTelp, noReg, bidang, aktif);
    }

    private synchronized void saveToFile() {
        if (filePath == null) return;
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            for (Dokter d : fileStorage.values()) {
                String line = String.join("|",
                        escape(d.getId()),
                        escape(d.getNama()),
                        escape(d.getJenisKelamin() != null ? d.getJenisKelamin().name() : ""),
                        escape(d.getAlamat()),
                        escape(d.getNoTelepon()),
                        escape(d.getNomorRegistrasi()),
                        escape(d.getBidangPelayanan()),
                        String.valueOf(d.isAktif())
                );
                writer.println(line);
            }
        } catch (IOException e) {
            System.err.println("Gagal menyimpan data dokter ke file: " + e.getMessage());
        }
    }

    private synchronized void loadFromFile() {
        if (filePath == null) return;
        fileStorage.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length >= 8) {
                    String id = unescape(parts[0]);
                    String nama = unescape(parts[1]);
                    JenisKelamin jk = JenisKelamin.fromString(unescape(parts[2]));
                    String alamat = unescape(parts[3]);
                    String noTelp = unescape(parts[4]);
                    String noReg = unescape(parts[5]);
                    String bidang = unescape(parts[6]);
                    boolean aktif = Boolean.parseBoolean(parts[7]);

                    Dokter d = new Dokter(id, nama, jk, alamat, noTelp, noReg, bidang, aktif);
                    fileStorage.put(id, d);
                    updateCounterFromId(id);
                }
            }
        } catch (IOException e) {
            System.err.println("Gagal memuat data dokter dari file: " + e.getMessage());
        }
    }

    private String escape(String val) {
        if (val == null) return "";
        return val.replace("\n", "\\n").replace("|", "/");
    }

    private String unescape(String val) {
        if (val == null) return "";
        return val.replace("\\n", "\n");
    }
}

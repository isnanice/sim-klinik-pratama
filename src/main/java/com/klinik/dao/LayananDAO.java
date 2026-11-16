package com.klinik.dao;

import com.klinik.model.Layanan;
import com.klinik.model.LayananSpesialis;
import com.klinik.model.LayananUmum;
import com.klinik.util.DatabaseConnection;
import com.klinik.util.PesanDb;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data Access Object untuk entitas Layanan.
 * Terintegrasi dengan database PostgreSQL (default) serta mendukung mode file untuk pengujian.
 */
public class LayananDAO implements GenericDAO<Layanan, String> {
    private final String dataDir;
    private final String filePath;
    private final Map<String, Layanan> storage = new LinkedHashMap<>();
    private int counter = 0;

    public LayananDAO() {
        this(null);
    }

    public LayananDAO(String dataDir) {
        this.dataDir = (dataDir != null && !dataDir.trim().isEmpty()) ? dataDir.trim() : null;
        this.filePath = (this.dataDir != null) ? this.dataDir + File.separator + "layanan.txt" : null;
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
        insert(new LayananUmum(generateNextId(), "Pemeriksaan Dokter Umum", 50000.0, "Konsultasi dan pemeriksaan fisik dasar", true));
        insert(new LayananSpesialis(generateNextId(), "Konsultasi Penyakit Dalam", 150000.0, "Pemeriksaan oleh dokter spesialis penyakit dalam", true, 50000.0));
        insert(new LayananUmum(generateNextId(), "Pemeriksaan Gigi Dasar", 75000.0, "Pemeriksaan kesehatan gigi dan mulut", true));
        insert(new LayananSpesialis(generateNextId(), "Konsultasi Anak", 150000.0, "Pemeriksaan tumbuh kembang anak oleh Sp.A", true, 50000.0));
        insert(new LayananUmum(generateNextId(), "Tindakan Medis Ringan", 100000.0, "Jahit luka ringan atau perawatan luka", true));
    }

    public synchronized String generateNextId() {
        if (!isFileMode()) {
            String sql = "SELECT fn_id('LYN', nextval('seq_layanan'))";
            try (Connection c = DatabaseConnection.getConnection();
                 Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            } catch (Exception ignored) {}
        }
        counter++;
        return String.format("LYN-%03d", counter);
    }

    private void updateCounterFromId(String id) {
        if (id != null && id.startsWith("LYN-")) {
            try {
                int val = Integer.parseInt(id.substring(4));
                if (val > counter) {
                    counter = val;
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    @Override
    public synchronized void insert(Layanan l) {
        if (l == null) return;

        if (isFileMode()) {
            if (l.getId() == null || l.getId().trim().isEmpty()) {
                l.setId(generateNextId());
            }
            updateCounterFromId(l.getId());
            storage.put(l.getId(), l);
            saveToFile();
            return;
        }

        String sql = "INSERT INTO layanan (id_layanan, nama_layanan, jenis_layanan, tarif, keterangan, status_aktif) "
                   + "VALUES (COALESCE(?, fn_id('LYN', nextval('seq_layanan'))), ?, ?, ?, ?, ?) "
                   + "RETURNING id_layanan";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            String jenis = (l instanceof LayananSpesialis) ? "SPESIALIS" : "UMUM";

            ps.setString(1, (l.getId() != null && !l.getId().isBlank()) ? l.getId() : null);
            ps.setString(2, l.getNamaLayanan());
            ps.setString(3, jenis);
            ps.setDouble(4, l.getTarifDasar());
            ps.setString(5, l.getKeterangan());
            ps.setBoolean(6, l.isAktif());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    l.setId(rs.getString("id_layanan"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Gagal menambah data layanan: " + PesanDb.untukPengguna(e), e);
        }
    }

    public void simpan(Layanan l) {
        insert(l);
    }

    @Override
    public synchronized void update(Layanan l) {
        if (l == null || l.getId() == null) return;

        if (isFileMode()) {
            storage.put(l.getId(), l);
            saveToFile();
            return;
        }

        String sql = "UPDATE layanan SET nama_layanan = ?, jenis_layanan = ?, tarif = ?, keterangan = ?, status_aktif = ? "
                   + "WHERE id_layanan = ?";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            String jenis = (l instanceof LayananSpesialis) ? "SPESIALIS" : "UMUM";

            ps.setString(1, l.getNamaLayanan());
            ps.setString(2, jenis);
            ps.setDouble(3, l.getTarifDasar());
            ps.setString(4, l.getKeterangan());
            ps.setBoolean(5, l.isAktif());
            ps.setString(6, l.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Gagal memperbarui data layanan: " + PesanDb.untukPengguna(e), e);
        }
    }

    public void ubah(Layanan l) {
        update(l);
    }

    @Override
    public synchronized void delete(String id) {
        // Aturan bisnis: layanan tidak dihapus permanen, melainkan dinonaktifkan
        toggleAktif(id, false);
    }

    public void hapus(String id) {
        delete(id);
    }

    public synchronized void toggleAktif(String id, boolean aktif) {
        if (id == null) return;

        if (isFileMode()) {
            Layanan l = storage.get(id);
            if (l != null) {
                l.setAktif(aktif);
                saveToFile();
            }
            return;
        }

        String sql = "UPDATE layanan SET status_aktif = ? WHERE id_layanan = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, aktif);
            ps.setString(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Gagal mengubah status aktif layanan: " + PesanDb.untukPengguna(e), e);
        }
    }

    @Override
    public synchronized Optional<Layanan> findById(String id) {
        if (id == null) return Optional.empty();

        if (isFileMode()) {
            return Optional.ofNullable(storage.get(id));
        }

        String sql = "SELECT * FROM layanan WHERE id_layanan = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToLayanan(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal mencari layanan by ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public synchronized List<Layanan> findAll() {
        if (isFileMode()) {
            return new ArrayList<>(storage.values());
        }

        List<Layanan> list = new ArrayList<>();
        String sql = "SELECT * FROM layanan ORDER BY id_layanan";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToLayanan(rs));
            }
        } catch (SQLException e) {
            System.err.println("Gagal memuat semua layanan: " + e.getMessage());
        }
        return list;
    }

    public synchronized List<Layanan> findActive() {
        if (isFileMode()) {
            return storage.values().stream()
                    .filter(Layanan::isAktif)
                    .collect(Collectors.toList());
        }

        List<Layanan> list = new ArrayList<>();
        String sql = "SELECT * FROM layanan WHERE status_aktif = TRUE ORDER BY id_layanan";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToLayanan(rs));
            }
        } catch (SQLException e) {
            System.err.println("Gagal memuat layanan aktif: " + e.getMessage());
        }
        return list;
    }

    public synchronized List<Layanan> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }

        if (isFileMode()) {
            String q = keyword.trim().toLowerCase();
            return storage.values().stream()
                    .filter(l -> (l.getNamaLayanan() != null && l.getNamaLayanan().toLowerCase().contains(q)) ||
                                 (l.getId() != null && l.getId().toLowerCase().contains(q)) ||
                                 (l.getJenisLayanan() != null && l.getJenisLayanan().toLowerCase().contains(q)))
                    .collect(Collectors.toList());
        }

        String q = "%" + keyword.trim().toLowerCase() + "%";
        String sql = "SELECT * FROM layanan WHERE LOWER(nama_layanan) LIKE ? OR LOWER(id_layanan) LIKE ? ORDER BY id_layanan";
        List<Layanan> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, q);
            ps.setString(2, q);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToLayanan(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal mencari layanan: " + e.getMessage());
        }
        return list;
    }

    public List<Layanan> cari(String keyword) {
        return search(keyword);
    }

    private Layanan mapResultSetToLayanan(ResultSet rs) throws SQLException {
        String id = rs.getString("id_layanan");
        String nama = rs.getString("nama_layanan");
        String jenis = rs.getString("jenis_layanan");
        double tarif = rs.getDouble("tarif");
        String ket = rs.getString("keterangan");
        boolean aktif = rs.getBoolean("status_aktif");

        if ("SPESIALIS".equalsIgnoreCase(jenis)) {
            return new LayananSpesialis(id, nama, tarif, ket, aktif);
        } else {
            return new LayananUmum(id, nama, tarif, ket, aktif);
        }
    }

    private synchronized void saveToFile() {
        if (filePath == null) return;
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            for (Layanan l : storage.values()) {
                double extraFee = 0.0;
                if (l instanceof LayananSpesialis) {
                    extraFee = ((LayananSpesialis) l).getBiayaKonsultasiSpesialis();
                }
                String line = String.join("|",
                        escape(l.getId()),
                        escape(l.getJenisLayanan()),
                        escape(l.getNamaLayanan()),
                        String.valueOf(l.getTarifDasar()),
                        escape(l.getKeterangan()),
                        String.valueOf(l.isAktif()),
                        String.valueOf(extraFee)
                );
                writer.println(line);
            }
        } catch (IOException e) {
            System.err.println("Gagal menyimpan data layanan ke file: " + e.getMessage());
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
                if (parts.length >= 6) {
                    String id = unescape(parts[0]);
                    String jenis = unescape(parts[1]);
                    String nama = unescape(parts[2]);
                    double tarif = Double.parseDouble(parts[3]);
                    String ket = unescape(parts[4]);
                    boolean aktif = Boolean.parseBoolean(parts[5]);
                    double extra = (parts.length >= 7 && !parts[6].isEmpty()) ? Double.parseDouble(parts[6]) : 50000.0;

                    Layanan l;
                    if ("Spesialis".equalsIgnoreCase(jenis)) {
                        l = new LayananSpesialis(id, nama, tarif, ket, aktif, extra);
                    } else {
                        l = new LayananUmum(id, nama, tarif, ket, aktif);
                    }
                    storage.put(id, l);
                    updateCounterFromId(id);
                }
            }
        } catch (IOException e) {
            System.err.println("Gagal memuat data layanan dari file: " + e.getMessage());
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

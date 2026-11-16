package com.klinik.dao;

import com.klinik.exception.DataNotFoundException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.model.JenisKelamin;
import com.klinik.model.Petugas;
import com.klinik.model.Role;
import com.klinik.util.DatabaseConnection;
import com.klinik.util.PasswordUtil;
import com.klinik.util.PesanDb;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data Access Object untuk entitas Petugas.
 * Terintegrasi dengan database PostgreSQL (default) serta mendukung mode file untuk pengujian.
 */
public class PetugasDAO implements GenericDAO<Petugas, String> {
    private final String dataDir;
    private final String filePath;
    private final Map<String, Petugas> storage = new LinkedHashMap<>();
    private int counter = 0;

    public PetugasDAO() {
        this(null);
    }

    public PetugasDAO(String dataDir) {
        this.dataDir = (dataDir != null && !dataDir.trim().isEmpty()) ? dataDir.trim() : null;
        this.filePath = (this.dataDir != null) ? this.dataDir + File.separator + "petugas.txt" : null;
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
            ensureDefaultAccounts();
        } else {
            seedInitialData();
            saveToFile();
        }
    }

    private void ensureDefaultAccounts() {
        boolean changed = false;
        if (!cariByUsername("admin").isPresent()) {
            insert(new Petugas(generateNextId(), "Siti Rahma", JenisKelamin.PEREMPUAN,
                    "Jl. Kaliurang Km 5, Sleman", "081234567801", true,
                    "admin", PasswordUtil.hashPassword("admin123"), Role.ADMIN, ""));
            changed = true;
        }
        if (!cariByUsername("petugas01").isPresent()) {
            insert(new Petugas(generateNextId(), "Rudi Hartono", JenisKelamin.LAKI_LAKI,
                    "Jl. Gejayan No. 15, Yogyakarta", "081234567802", true,
                    "petugas01", PasswordUtil.hashPassword("petugas123"), Role.PETUGAS, ""));
            changed = true;
        }
        if (!cariByUsername("dokter01").isPresent()) {
            insert(new Petugas(generateNextId(), "dr. Ani Wijaya", JenisKelamin.PEREMPUAN,
                    "Jl. Malioboro No. 10, Yogyakarta", "081234567890", true,
                    "dokter01", PasswordUtil.hashPassword("dokter123"), Role.DOKTER, "DKT-001"));
            changed = true;
        }
        if (!cariByUsername("dokter02").isPresent()) {
            insert(new Petugas(generateNextId(), "dr. Hadi Nugroho, Sp.PD", JenisKelamin.LAKI_LAKI,
                    "Jl. Kaliurang Km 5, Sleman", "081298765432", true,
                    "dokter02", PasswordUtil.hashPassword("dokter123"), Role.DOKTER, "DKT-002"));
            changed = true;
        }
        if (changed) {
            saveToFile();
        }
    }

    private void seedInitialData() {
        insert(new Petugas(
                generateNextId(),
                "Siti Rahma",
                JenisKelamin.PEREMPUAN,
                "Jl. Kaliurang Km 5, Sleman",
                "081234567801",
                true,
                "admin",
                PasswordUtil.hashPassword("admin123"),
                Role.ADMIN,
                ""
        ));
        insert(new Petugas(
                generateNextId(),
                "Rudi Hartono",
                JenisKelamin.LAKI_LAKI,
                "Jl. Gejayan No. 15, Yogyakarta",
                "081234567802",
                true,
                "petugas01",
                PasswordUtil.hashPassword("petugas123"),
                Role.PETUGAS,
                ""
        ));
        insert(new Petugas(
                generateNextId(),
                "Rudi Hartono",
                JenisKelamin.LAKI_LAKI,
                "Jl. Gejayan No. 15, Yogyakarta",
                "081234567802",
                true,
                "petugas",
                PasswordUtil.hashPassword("petugas123"),
                Role.PETUGAS,
                ""
        ));
        insert(new Petugas(
                generateNextId(),
                "dr. Ani Wijaya",
                JenisKelamin.PEREMPUAN,
                "Jl. Malioboro No. 10, Yogyakarta",
                "081234567890",
                true,
                "dokter01",
                PasswordUtil.hashPassword("dokter123"),
                Role.DOKTER,
                "DKT-001"
        ));
        insert(new Petugas(
                generateNextId(),
                "dr. Hadi Nugroho, Sp.PD",
                JenisKelamin.LAKI_LAKI,
                "Jl. Kaliurang Km 5, Sleman",
                "081298765432",
                true,
                "dokter02",
                PasswordUtil.hashPassword("dokter123"),
                Role.DOKTER,
                "DKT-002"
        ));
        insert(new Petugas(
                generateNextId(),
                "dr. Ani Wijaya",
                JenisKelamin.PEREMPUAN,
                "Jl. Malioboro No. 10, Yogyakarta",
                "081234567890",
                true,
                "dokter",
                PasswordUtil.hashPassword("dokter123"),
                Role.DOKTER,
                "DKT-001"
        ));
        insert(new Petugas(
                generateNextId(),
                "Andi Pratama (Nonaktif)",
                JenisKelamin.LAKI_LAKI,
                "Jl. Bantul Km 4, Yogyakarta",
                "081234567899",
                false,
                "nonaktif",
                PasswordUtil.hashPassword("nonaktif123"),
                Role.PETUGAS,
                ""
        ));
    }

    public synchronized String generateNextId() {
        if (!isFileMode()) {
            String sql = "SELECT fn_id('PTG', nextval('seq_petugas'))";
            try (Connection c = DatabaseConnection.getConnection();
                 Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            } catch (Exception ignored) {}
        }
        counter++;
        return String.format("PTG-%03d", counter);
    }

    private void updateCounterFromId(String id) {
        if (id != null && id.startsWith("PTG-")) {
            try {
                int val = Integer.parseInt(id.substring(4));
                if (val > counter) {
                    counter = val;
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    @Override
    public synchronized void insert(Petugas p) {
        if (p == null) return;

        String hash = p.getPassword();
        if (hash == null || !hash.contains(":")) {
            hash = PasswordUtil.hash(hash != null ? hash : "klinik123");
            try {
                p.setPassword(hash);
            } catch (DataTidakValidException ignored) {}
        }

        if (isFileMode()) {
            if (p.getId() == null || p.getId().trim().isEmpty()) {
                p.setId(generateNextId());
            }
            updateCounterFromId(p.getId());
            storage.put(p.getId(), p);
            saveToFile();
            return;
        }

        String sql = "INSERT INTO petugas (id_petugas, nama, alamat, no_telepon, username, password_hash, role, id_dokter, status_aktif) "
                   + "VALUES (COALESCE(?, fn_id('PTG', nextval('seq_petugas'))), ?, ?, ?, ?, ?, ?, ?, ?) "
                   + "RETURNING id_petugas";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, (p.getId() != null && !p.getId().isBlank()) ? p.getId() : null);
            ps.setString(2, p.getNama());
            ps.setString(3, p.getAlamat());
            ps.setString(4, p.getNoTelepon());
            ps.setString(5, p.getUsername());
            ps.setString(6, hash);
            ps.setString(7, p.getRole() != null ? p.getRole().name() : "PETUGAS");
            if (p.getIdDokter() != null && !p.getIdDokter().trim().isEmpty()) {
                ps.setString(8, p.getIdDokter().trim());
            } else {
                ps.setNull(8, Types.VARCHAR);
            }
            ps.setBoolean(9, p.isAktif());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    p.setId(rs.getString("id_petugas"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Gagal menambah data petugas: " + PesanDb.untukPengguna(e), e);
        }
    }

    public void simpan(Petugas p) {
        insert(p);
    }

    @Override
    public synchronized void update(Petugas p) {
        if (p == null || p.getId() == null) return;

        if (isFileMode()) {
            storage.put(p.getId(), p);
            saveToFile();
            return;
        }

        String sql = "UPDATE petugas SET nama = ?, alamat = ?, no_telepon = ?, username = ?, role = ?, id_dokter = ?, status_aktif = ? "
                   + "WHERE id_petugas = ?";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, p.getNama());
            ps.setString(2, p.getAlamat());
            ps.setString(3, p.getNoTelepon());
            ps.setString(4, p.getUsername());
            ps.setString(5, p.getRole() != null ? p.getRole().name() : "PETUGAS");
            if (p.getIdDokter() != null && !p.getIdDokter().trim().isEmpty()) {
                ps.setString(6, p.getIdDokter().trim());
            } else {
                ps.setNull(6, Types.VARCHAR);
            }
            ps.setBoolean(7, p.isAktif());
            ps.setString(8, p.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Gagal memperbarui data petugas: " + PesanDb.untukPengguna(e), e);
        }
    }

    public void ubah(Petugas p) {
        update(p);
    }

    @Override
    public synchronized void delete(String id) {
        if (id == null) return;

        if (isFileMode()) {
            storage.remove(id);
            saveToFile();
            return;
        }

        String sql = "DELETE FROM petugas WHERE id_petugas = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Gagal menghapus data petugas: " + PesanDb.untukPengguna(e), e);
        }
    }

    public void hapus(String id) {
        delete(id);
    }

    public synchronized void toggleAktif(String id, boolean aktif) {
        if (id == null) return;

        if (isFileMode()) {
            Petugas petugas = storage.get(id);
            if (petugas != null) {
                petugas.setAktif(aktif);
                saveToFile();
            }
            return;
        }

        String sql = "UPDATE petugas SET status_aktif = ? WHERE id_petugas = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, aktif);
            ps.setString(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Gagal mengubah status aktif petugas: " + PesanDb.untukPengguna(e), e);
        }
    }

    @Override
    public synchronized Optional<Petugas> findById(String id) {
        if (id == null) return Optional.empty();

        if (isFileMode()) {
            return Optional.ofNullable(storage.get(id));
        }

        String sql = "SELECT * FROM petugas WHERE id_petugas = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPetugas(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal mencari petugas by ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    public synchronized Optional<Petugas> cariByUsername(String username) {
        return findByUsername(username);
    }

    public synchronized Optional<Petugas> findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) return Optional.empty();

        if (isFileMode()) {
            String clean = username.trim();
            return storage.values().stream()
                    .filter(p -> p.getUsername() != null && p.getUsername().equalsIgnoreCase(clean))
                    .findFirst();
        }

        String sql = "SELECT * FROM petugas WHERE LOWER(username) = LOWER(?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPetugas(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal mencari petugas by username: " + e.getMessage());
        }
        return Optional.empty();
    }

    public synchronized void validasiStatusAktif(Petugas petugas) throws DataNotFoundException, DataTidakValidException {
        if (petugas == null) {
            throw new DataNotFoundException("Data petugas tidak ditemukan.");
        }
        if (!petugas.isAktif()) {
            throw new DataTidakValidException("Status Petugas",
                    "Akun petugas '" + petugas.getUsername() + "' berstatus nonaktif dan tidak dapat mengakses sistem.");
        }
    }

    public synchronized Petugas cariAktifByUsername(String username) throws DataNotFoundException, DataTidakValidException {
        Petugas petugas = findByUsername(username)
                .orElseThrow(() -> new DataNotFoundException("Petugas dengan username '" + username + "' tidak ditemukan."));
        validasiStatusAktif(petugas);
        return petugas;
    }

    @Override
    public synchronized List<Petugas> findAll() {
        if (isFileMode()) {
            return new ArrayList<>(storage.values());
        }

        List<Petugas> list = new ArrayList<>();
        String sql = "SELECT * FROM petugas ORDER BY id_petugas";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToPetugas(rs));
            }
        } catch (SQLException e) {
            System.err.println("Gagal memuat semua petugas: " + e.getMessage());
        }
        return list;
    }

    public synchronized List<Petugas> findActive() {
        if (isFileMode()) {
            return storage.values().stream()
                    .filter(Petugas::isAktif)
                    .collect(Collectors.toList());
        }

        List<Petugas> list = new ArrayList<>();
        String sql = "SELECT * FROM petugas WHERE status_aktif = TRUE ORDER BY id_petugas";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToPetugas(rs));
            }
        } catch (SQLException e) {
            System.err.println("Gagal memuat petugas aktif: " + e.getMessage());
        }
        return list;
    }

    private Petugas mapResultSetToPetugas(ResultSet rs) throws SQLException {
        String id = rs.getString("id_petugas");
        String nama = rs.getString("nama");
        String alamat = rs.getString("alamat");
        String noTelp = rs.getString("no_telepon");
        String username = rs.getString("username");
        String pwdHash = rs.getString("password_hash");
        String roleStr = rs.getString("role");
        String idDokter = rs.getString("id_dokter");
        boolean aktif = rs.getBoolean("status_aktif");

        Role role = Role.PETUGAS;
        try {
            if (roleStr != null) {
                role = Role.valueOf(roleStr.trim().toUpperCase());
            }
        } catch (Exception ignored) {}

        return new Petugas(id, nama, JenisKelamin.LAKI_LAKI, alamat, noTelp, aktif, username, pwdHash, role, idDokter != null ? idDokter : "");
    }

    private synchronized void saveToFile() {
        if (filePath == null) return;
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            for (Petugas p : storage.values()) {
                String line = String.join("|",
                        escape(p.getId()),
                        escape(p.getNama()),
                        p.getJenisKelamin() != null ? p.getJenisKelamin().name() : "",
                        escape(p.getAlamat()),
                        escape(p.getNoTelepon()),
                        String.valueOf(p.isAktif()),
                        escape(p.getUsername()),
                        p.getPassword() != null ? p.getPassword() : "",
                        p.getRole() != null ? p.getRole().name() : Role.PETUGAS.name(),
                        escape(p.getIdDokter())
                );
                writer.println(line);
            }
        } catch (IOException e) {
            System.err.println("Gagal menyimpan data petugas ke file: " + e.getMessage());
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
                    String id = unescape(parts[0]);
                    String nama = unescape(parts[1]);
                    JenisKelamin jk = JenisKelamin.fromString(parts[2]);
                    String alamat = unescape(parts[3]);
                    String noTelp = unescape(parts[4]);
                    boolean aktif = Boolean.parseBoolean(parts[5]);
                    String username = unescape(parts[6]);
                    String password = parts[7];
                    Role role = Role.fromString(parts[8]);
                    String idDokter = parts.length >= 10 ? unescape(parts[9]) : "";
                    if (role == Role.DOKTER && idDokter.isEmpty()) {
                        idDokter = "DKT-001";
                    }

                    Petugas p = new Petugas(id, nama, jk, alamat, noTelp, aktif, username, password, role, idDokter);
                    storage.put(id, p);
                    updateCounterFromId(id);
                }
            }
        } catch (IOException e) {
            System.err.println("Gagal memuat data petugas dari file: " + e.getMessage());
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

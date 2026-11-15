package com.klinik.dao;

import com.klinik.exception.DataNotFoundException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.model.JenisKelamin;
import com.klinik.model.Petugas;
import com.klinik.model.Role;
import com.klinik.util.PasswordUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Data Access Object untuk entitas Petugas/Pengguna Klinik dengan persistensi file (data/petugas.txt).
 * Menyediakan pencarian berdasarkan username, validasi status aktif, dan operasi CRUD dasar petugas.
 */
public class PetugasDAO implements GenericDAO<Petugas, String> {
    private static final String DATA_DIR = "data";
    private static final String FILE_PATH = DATA_DIR + File.separator + "petugas.txt";

    private final Map<String, Petugas> storage = new LinkedHashMap<>();
    private int counter = 0;

    public PetugasDAO() {
        initStorage();
    }

    private void initStorage() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(FILE_PATH);
        if (file.exists()) {
            loadFromFile();
        } else {
            seedInitialData();
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
                Role.ADMIN
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
                Role.PETUGAS
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
                Role.PETUGAS
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
                Role.DOKTER
        ));
        insert(new Petugas(
                generateNextId(),
                "dr. Hadi Nugroho, Sp.PD",
                JenisKelamin.LAKI_LAKI,
                "Jl. Kaliurang Km 5, Sleman",
                "081298765432",
                true,
                "dokter",
                PasswordUtil.hashPassword("dokter123"),
                Role.DOKTER
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
                Role.PETUGAS
        ));
    }

    public synchronized String generateNextId() {
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
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @Override
    public synchronized void insert(Petugas entity) {
        if (entity == null) {
            return;
        }
        if (entity.getId() == null || entity.getId().trim().isEmpty()) {
            entity.setId(generateNextId());
        } else {
            updateCounterFromId(entity.getId());
        }
        ensurePasswordHashed(entity);
        storage.put(entity.getId(), entity);
        saveToFile();
    }

    @Override
    public synchronized void update(Petugas entity) {
        if (entity != null && entity.getId() != null) {
            ensurePasswordHashed(entity);
            storage.put(entity.getId(), entity);
            saveToFile();
        }
    }

    @Override
    public synchronized void delete(String id) {
        if (id != null) {
            storage.remove(id);
            saveToFile();
        }
    }

    public synchronized void toggleAktif(String id, boolean aktif) {
        Petugas petugas = storage.get(id);
        if (petugas != null) {
            petugas.setAktif(aktif);
            saveToFile();
        }
    }

    @Override
    public synchronized Optional<Petugas> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public synchronized List<Petugas> findAll() {
        return new ArrayList<>(storage.values());
    }

    public synchronized List<Petugas> findActive() {
        return storage.values().stream()
                .filter(Petugas::isAktif)
                .collect(Collectors.toList());
    }

    /**
     * Mencari petugas berdasarkan username (case-insensitive).
     */
    public synchronized Optional<Petugas> cariByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return Optional.empty();
        }
        String clean = username.trim();
        return storage.values().stream()
                .filter(p -> p.getUsername() != null && p.getUsername().equalsIgnoreCase(clean))
                .findFirst();
    }

    public synchronized Optional<Petugas> findByUsername(String username) {
        return cariByUsername(username);
    }

    /**
     * Memvalidasi bahwa objek petugas ditemukan dan memiliki status aktif (status_aktif = true).
     */
    public synchronized void validasiStatusAktif(Petugas petugas) throws DataNotFoundException, DataTidakValidException {
        if (petugas == null) {
            throw new DataNotFoundException("Data petugas tidak ditemukan.");
        }
        if (!petugas.isAktif()) {
            throw new DataTidakValidException("Status Petugas",
                    "Akun petugas '" + petugas.getUsername() + "' berstatus nonaktif dan tidak dapat mengakses sistem.");
        }
    }

    /**
     * Mencari petugas berdasarkan username sekaligus memvalidasi status_aktif.
     */
    public synchronized Petugas cariAktifByUsername(String username) throws DataNotFoundException, DataTidakValidException {
        Petugas petugas = cariByUsername(username)
                .orElseThrow(() -> new DataNotFoundException("Petugas dengan username '" + username + "' tidak ditemukan."));
        validasiStatusAktif(petugas);
        return petugas;
    }

    private void ensurePasswordHashed(Petugas petugas) {
        String pwd = petugas.getPassword();
        if (pwd != null && !pwd.isEmpty() && !pwd.contains(":")) {
            petugas.setPassword(PasswordUtil.hashPassword(pwd));
        }
    }

    private synchronized void saveToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_PATH))) {
            for (Petugas p : storage.values()) {
                // Format: id|nama|jenisKelamin|alamat|noTelepon|aktif|username|password|role
                String line = String.join("|",
                        escape(p.getId()),
                        escape(p.getNama()),
                        p.getJenisKelamin() != null ? p.getJenisKelamin().name() : "",
                        escape(p.getAlamat()),
                        escape(p.getNoTelepon()),
                        String.valueOf(p.isAktif()),
                        escape(p.getUsername()),
                        p.getPassword() != null ? p.getPassword() : "",
                        p.getRole() != null ? p.getRole().name() : Role.PETUGAS.name()
                );
                writer.println(line);
            }
        } catch (IOException e) {
            System.err.println("Gagal menyimpan data petugas: " + e.getMessage());
        }
    }

    private synchronized void loadFromFile() {
        storage.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
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

                    Petugas p = new Petugas(id, nama, jk, alamat, noTelp, aktif, username, password, role);
                    storage.put(id, p);
                    updateCounterFromId(id);
                }
            }
        } catch (IOException e) {
            System.err.println("Gagal memuat data petugas: " + e.getMessage());
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

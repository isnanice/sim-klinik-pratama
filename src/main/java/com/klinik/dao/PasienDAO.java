package com.klinik.dao;

import com.klinik.model.JenisKelamin;
import com.klinik.model.Pasien;

import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data Access Object untuk entitas Pasien dengan persistensi file teks/CSV.
 */
public class PasienDAO implements GenericDAO<Pasien, String> {
    private static final String DATA_DIR = "data";
    private static final String FILE_PATH = DATA_DIR + File.separator + "pasien.txt";
    private final Map<String, Pasien> storage = new LinkedHashMap<>();
    private int counter = 0;

    public PasienDAO() {
        initStorage();
    }

    private void initStorage() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) dir.mkdirs();
        File file = new File(FILE_PATH);
        if (file.exists()) {
            loadFromFile();
        } else {
            seedInitialData();
            saveToFile();
        }
    }

    private void seedInitialData() {
        insert(new Pasien(generateNextId(), "Budi Santoso", JenisKelamin.LAKI_LAKI, LocalDate.of(1990, 5, 12),
                "Jl. Malioboro No. 12, Yogyakarta", "081234567890", "0001234567890", LocalDate.now(), true));
        insert(new Pasien(generateNextId(), "Sari Dewi", JenisKelamin.PEREMPUAN, LocalDate.of(1985, 9, 3),
                "Jl. Gejayan No. 44, Sleman", "081298765432", "0001234567891", LocalDate.now(), true));
        insert(new Pasien(generateNextId(), "Agus Prasetyo", JenisKelamin.LAKI_LAKI, LocalDate.of(2001, 1, 21),
                "Jl. Palagan No. 8, Sleman", "085711223344", "0001234567892", LocalDate.now(), true));
        insert(new Pasien(generateNextId(), "Rina Wulandari", JenisKelamin.PEREMPUAN, LocalDate.of(1978, 7, 14),
                "Jl. Kusumanegara No. 99, Yogyakarta", "082133445566", "0001234567893", LocalDate.now(), false));
        insert(new Pasien(generateNextId(), "Dimas Aditya", JenisKelamin.LAKI_LAKI, LocalDate.of(1995, 11, 30),
                "Jl. Wonosari Km 4, Bantul", "087855667788", "0001234567894", LocalDate.now(), true));
    }

    public synchronized String generateNextId() {
        counter++;
        return String.format("PSN-%03d", counter);
    }

    private void updateCounterFromId(String id) {
        if (id != null && id.startsWith("PSN-")) {
            try {
                int val = Integer.parseInt(id.substring(4));
                if (val > counter) counter = val;
            } catch (NumberFormatException ignored) {}
        }
    }

    @Override
    public synchronized void insert(Pasien pasien) {
        if (pasien.getId() == null || pasien.getId().trim().isEmpty()) {
            pasien.setId(generateNextId());
        } else {
            updateCounterFromId(pasien.getId());
        }
        storage.put(pasien.getId(), pasien);
        saveToFile();
    }

    @Override
    public synchronized void update(Pasien pasien) {
        if (pasien != null && pasien.getId() != null) {
            storage.put(pasien.getId(), pasien);
            saveToFile();
        }
    }

    @Override
    public synchronized void delete(String id) {
        storage.remove(id);
        saveToFile();
    }

    public synchronized void toggleAktif(String id, boolean aktif) {
        Pasien pasien = storage.get(id);
        if (pasien != null) {
            pasien.setAktif(aktif);
            saveToFile();
        }
    }

    @Override
    public synchronized Optional<Pasien> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public synchronized List<Pasien> findAll() {
        return new ArrayList<>(storage.values());
    }

    public synchronized List<Pasien> findActive() {
        return storage.values().stream()
                .filter(Pasien::isAktif)
                .collect(Collectors.toList());
    }

    private synchronized void saveToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_PATH))) {
            for (Pasien p : storage.values()) {
                String line = String.join("|",
                        p.getId() != null ? p.getId() : "",
                        p.getNama() != null ? p.getNama() : "",
                        p.getJenisKelamin() != null ? p.getJenisKelamin().name() : "",
                        p.getTanggalLahir() != null ? p.getTanggalLahir().toString() : "",
                        p.getAlamat() != null ? p.getAlamat() : "",
                        p.getNoTelepon() != null ? p.getNoTelepon() : "",
                        p.getNoBpjs() != null ? p.getNoBpjs() : "",
                        p.getTanggalRegistrasi() != null ? p.getTanggalRegistrasi().toString() : "",
                        String.valueOf(p.isAktif())
                );
                writer.println(line);
            }
        } catch (IOException e) {
            System.err.println("Gagal menyimpan pasien: " + e.getMessage());
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
                    String id = parts[0];
                    String nama = parts[1];
                    JenisKelamin jk = JenisKelamin.fromString(parts[2]);
                    LocalDate tglLahir = parts[3].isEmpty() ? null : LocalDate.parse(parts[3]);
                    String alamat = parts[4];
                    String noTelp = parts[5];
                    String noBpjs = parts[6];
                    LocalDate tglReg = parts[7].isEmpty() ? LocalDate.now() : LocalDate.parse(parts[7]);
                    boolean aktif = Boolean.parseBoolean(parts[8]);

                    Pasien p = new Pasien(id, nama, jk, tglLahir, alamat, noTelp, noBpjs, tglReg, aktif);
                    storage.put(id, p);
                    updateCounterFromId(id);
                }
            }
        } catch (Exception e) {
            System.err.println("Gagal memuat pasien: " + e.getMessage());
        }
    }
}


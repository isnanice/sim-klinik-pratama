package com.klinik.dao;

import com.klinik.model.*;

import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data Access Object untuk entitas Pembayaran dengan persistensi file teks/CSV.
 */
public class PembayaranDAO implements GenericDAO<Pembayaran, String> {
    private static final String DEFAULT_DATA_DIR = "data";
    private final String dataDir;
    private final String filePath;
    private final Map<String, Pembayaran> storage = new LinkedHashMap<>();
    private final PemeriksaanDAO pemeriksaanDAO;
    private int counter = 0;

    public PembayaranDAO(PemeriksaanDAO pemeriksaanDAO) {
        this(pemeriksaanDAO, System.getProperty("klinik.data.dir", DEFAULT_DATA_DIR));
    }

    public PembayaranDAO(PemeriksaanDAO pemeriksaanDAO, String dataDir) {
        this.pemeriksaanDAO = pemeriksaanDAO;
        this.dataDir = (dataDir != null && !dataDir.trim().isEmpty()) ? dataDir.trim() : DEFAULT_DATA_DIR;
        this.filePath = this.dataDir + File.separator + "pembayaran.txt";
        initStorage();
    }

    private void initStorage() {
        File dir = new File(dataDir);
        if (!dir.exists()) dir.mkdirs();
        File file = new File(filePath);
        if (file.exists()) {
            loadFromFile();
        } else {
            saveToFile();
        }
    }

    public synchronized String generateNextId() {
        counter++;
        return String.format("BYR-%03d", counter);
    }

    private void updateCounterFromId(String id) {
        if (id != null && id.startsWith("BYR-")) {
            try {
                int val = Integer.parseInt(id.substring(4));
                if (val > counter) counter = val;
            } catch (NumberFormatException ignored) {}
        }
    }

    @Override
    public synchronized void insert(Pembayaran entity) {
        if (entity.getId() == null || entity.getId().trim().isEmpty()) {
            entity.setId(generateNextId());
        } else {
            updateCounterFromId(entity.getId());
        }
        storage.put(entity.getId(), entity);
        saveToFile();
    }

    @Override
    public synchronized void update(Pembayaran entity) {
        if (entity != null && entity.getId() != null) {
            storage.put(entity.getId(), entity);
            saveToFile();
        }
    }

    @Override
    public synchronized void delete(String id) {
        storage.remove(id);
        saveToFile();
    }

    @Override
    public synchronized Optional<Pembayaran> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public synchronized List<Pembayaran> findAll() {
        return new ArrayList<>(storage.values());
    }

    public synchronized Optional<Pembayaran> findByPemeriksaanId(String idPemeriksaan) {
        if (idPemeriksaan == null) return Optional.empty();
        return storage.values().stream()
                .filter(b -> b.getPemeriksaan() != null && idPemeriksaan.equalsIgnoreCase(b.getPemeriksaan().getId()))
                .findFirst();
    }

    private synchronized void saveToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            for (Pembayaran b : storage.values()) {
                String line = String.join("|",
                        b.getId() != null ? b.getId() : "",
                        (b.getPemeriksaan() != null && b.getPemeriksaan().getId() != null) ? b.getPemeriksaan().getId() : "",
                        b.getTanggalPembayaran() != null ? b.getTanggalPembayaran().toString() : "",
                        String.valueOf(b.getBiayaTambahan()),
                        String.valueOf(b.getTotalBiaya()),
                        b.getMetodePembayaran() != null ? b.getMetodePembayaran().name() : "",
                        b.getStatusPembayaran() != null ? b.getStatusPembayaran().name() : ""
                );
                writer.println(line);
            }
        } catch (IOException e) {
            System.err.println("Gagal menyimpan pembayaran: " + e.getMessage());
        }
    }

    private synchronized void loadFromFile() {
        storage.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length >= 7) {
                    String id = parts[0];
                    String idPemeriksaan = parts[1];
                    LocalDate tgl = parts[2].isEmpty() ? LocalDate.now() : LocalDate.parse(parts[2]);
                    double tambahan = Double.parseDouble(parts[3]);
                    double total = Double.parseDouble(parts[4]);
                    MetodePembayaran metode = MetodePembayaran.fromString(parts[5]);
                    StatusPembayaran status = StatusPembayaran.fromString(parts[6]);

                    Pemeriksaan pemeriksaan = pemeriksaanDAO.findById(idPemeriksaan).orElse(null);
                    Pembayaran b = new Pembayaran(id, pemeriksaan, tgl, tambahan, total, metode, status);
                    storage.put(id, b);
                    updateCounterFromId(id);
                }
            }
        } catch (Exception e) {
            System.err.println("Gagal memuat pembayaran: " + e.getMessage());
        }
    }
}


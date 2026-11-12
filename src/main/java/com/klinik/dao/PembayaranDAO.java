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
    private static final String DATA_DIR = "data";
    private static final String FILE_PATH = DATA_DIR + File.separator + "pembayaran.txt";
    private final Map<String, Pembayaran> storage = new LinkedHashMap<>();
    private final PemeriksaanDAO pemeriksaanDAO;
    private int counter = 0;

    public PembayaranDAO(PemeriksaanDAO pemeriksaanDAO) {
        this.pemeriksaanDAO = pemeriksaanDAO;
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
        List<Pemeriksaan> list = pemeriksaanDAO.findAll();
        if (!list.isEmpty()) {
            Pemeriksaan p = list.get(0);
            double total = p.getLayanan() != null ? p.getLayanan().hitungTarif(0.0) : 75000.0;
            Pembayaran byr = new Pembayaran(generateNextId(), p, LocalDate.now(), 0.0, total, MetodePembayaran.TUNAI, StatusPembayaran.LUNAS);
            insert(byr);
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
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_PATH))) {
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
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
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


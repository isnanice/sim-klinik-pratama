package com.klinik.dao;

import com.klinik.model.Layanan;
import com.klinik.model.LayananSpesialis;
import com.klinik.model.LayananUmum;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data Access Object untuk entitas Layanan (LayananUmum dan LayananSpesialis).
 * Menyimpan data secara persisten ke file teks/CSV di folder data/layanan.txt.
 */
public class LayananDAO implements GenericDAO<Layanan, String> {
    private static final String DATA_DIR = "data";
    private static final String FILE_PATH = DATA_DIR + File.separator + "layanan.txt";
    private final Map<String, Layanan> storage = new LinkedHashMap<>();
    private int counter = 0;

    public LayananDAO() {
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
        insert(new LayananUmum(generateNextId(), "Konsultasi Umum", 75000.0, "Pemeriksaan dan konsultasi dokter umum", true));
        insert(new LayananUmum(generateNextId(), "Pemeriksaan Gigi", 120000.0, "Pembersihan karang dan pemeriksaan gigi", true));
        insert(new LayananSpesialis(generateNextId(), "Konsultasi Penyakit Dalam", 150000.0, "Konsultasi dan pemeriksaan spesialis penyakit dalam", true, 50000.0));
        insert(new LayananSpesialis(generateNextId(), "Konsultasi Anak Spesialis", 140000.0, "Pemeriksaan tumbuh kembang dan penyakit anak", true, 50000.0));
        insert(new LayananUmum(generateNextId(), "Suntik Vitamin C", 50000.0, "Injeksi vitamin penambah imun", false));
    }

    public synchronized String generateNextId() {
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
    public synchronized void insert(Layanan layanan) {
        if (layanan.getId() == null || layanan.getId().trim().isEmpty()) {
            layanan.setId(generateNextId());
        } else {
            updateCounterFromId(layanan.getId());
        }
        storage.put(layanan.getId(), layanan);
        saveToFile();
    }

    @Override
    public synchronized void update(Layanan layanan) {
        if (layanan != null && layanan.getId() != null) {
            storage.put(layanan.getId(), layanan);
            saveToFile();
        }
    }

    @Override
    public synchronized void delete(String id) {
        // Layanan dalam aturan bisnis tidak dihapus permanen melainkan dinonaktifkan
        toggleAktif(id, false);
    }

    public synchronized void toggleAktif(String id, boolean aktif) {
        Layanan layanan = storage.get(id);
        if (layanan != null) {
            layanan.setAktif(aktif);
            saveToFile();
        }
    }

    @Override
    public synchronized Optional<Layanan> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public synchronized List<Layanan> findAll() {
        return new ArrayList<>(storage.values());
    }

    public synchronized List<Layanan> findActive() {
        return storage.values().stream()
                .filter(Layanan::isAktif)
                .collect(Collectors.toList());
    }

    public synchronized List<Layanan> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        String q = keyword.trim().toLowerCase();
        return storage.values().stream()
                .filter(l -> (l.getNamaLayanan() != null && l.getNamaLayanan().toLowerCase().contains(q)) ||
                             (l.getId() != null && l.getId().toLowerCase().contains(q)) ||
                             (l.getJenisLayanan() != null && l.getJenisLayanan().toLowerCase().contains(q)))
                .collect(Collectors.toList());
    }

    private synchronized void saveToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_PATH))) {
            for (Layanan l : storage.values()) {
                // Format: id|jenis|namaLayanan|tarifDasar|keterangan|aktif|extraFee
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
        storage.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
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
        } catch (Exception e) {
            System.err.println("Gagal memuat data layanan dari file: " + e.getMessage());
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


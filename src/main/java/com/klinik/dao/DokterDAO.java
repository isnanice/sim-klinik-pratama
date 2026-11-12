package com.klinik.dao;

import com.klinik.exception.DataMasihDipakaiException;
import com.klinik.model.Dokter;
import com.klinik.model.JenisKelamin;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data Access Object untuk entitas Dokter.
 * Menyimpan data secara persisten ke file teks/CSV di folder data/dokter.txt.
 */
public class DokterDAO implements GenericDAO<Dokter, String> {
    private static final String DATA_DIR = "data";
    private static final String FILE_PATH = DATA_DIR + File.separator + "dokter.txt";
    private final Map<String, Dokter> storage = new LinkedHashMap<>();
    private int counter = 0;

    public DokterDAO() {
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
    public synchronized void insert(Dokter dokter) {
        if (dokter.getId() == null || dokter.getId().trim().isEmpty()) {
            dokter.setId(generateNextId());
        } else {
            updateCounterFromId(dokter.getId());
        }
        storage.put(dokter.getId(), dokter);
        saveToFile();
    }

    @Override
    public synchronized void update(Dokter dokter) {
        if (dokter != null && dokter.getId() != null) {
            storage.put(dokter.getId(), dokter);
            saveToFile();
        }
    }

    @Override
    public synchronized void delete(String id) {
        storage.remove(id);
        saveToFile();
    }

    public synchronized void toggleAktif(String id, boolean aktif) {
        Dokter dokter = storage.get(id);
        if (dokter != null) {
            dokter.setAktif(aktif);
            saveToFile();
        }
    }

    @Override
    public synchronized Optional<Dokter> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public synchronized List<Dokter> findAll() {
        return new ArrayList<>(storage.values());
    }

    public synchronized List<Dokter> findActive() {
        return storage.values().stream()
                .filter(Dokter::isAktif)
                .collect(Collectors.toList());
    }

    public synchronized List<Dokter> search(String keyword, String criteria) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        String q = keyword.trim().toLowerCase();
        return storage.values().stream()
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

    private synchronized void saveToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_PATH))) {
            for (Dokter d : storage.values()) {
                // Format: id|nama|jenisKelamin|alamat|noTelepon|nomorRegistrasi|bidangPelayanan|aktif
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
        storage.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
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
                    storage.put(id, d);
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


package com.klinik.dao;

import com.klinik.model.Dokter;
import com.klinik.model.Pasien;
import com.klinik.model.Pemeriksaan;
import com.klinik.model.RekamMedis;

import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data Access Object untuk entitas RekamMedis dengan persistensi file teks/CSV.
 */
public class RekamMedisDAO implements GenericDAO<RekamMedis, String> {
    private static final String DATA_DIR = "data";
    private static final String FILE_PATH = DATA_DIR + File.separator + "rekam_medis.txt";
    private final Map<String, RekamMedis> storage = new LinkedHashMap<>();
    private final PemeriksaanDAO pemeriksaanDAO;
    private final PasienDAO pasienDAO;
    private final DokterDAO dokterDAO;
    private int counter = 0;

    public RekamMedisDAO(PemeriksaanDAO pemeriksaanDAO, PasienDAO pasienDAO, DokterDAO dokterDAO) {
        this.pemeriksaanDAO = pemeriksaanDAO;
        this.pasienDAO = pasienDAO;
        this.dokterDAO = dokterDAO;
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
            RekamMedis rm = new RekamMedis(generateNextId(), p, p.getPasien(), p.getDokter(),
                    p.getTanggalPemeriksaan(), "Infeksi saluran pernapasan akut (ISPA)",
                    "Pemberian obat simtomatik dan edukasi istirahat cukup",
                    "Paracetamol 500mg 3x1, Ambroxol 30mg 3x1",
                    "Kontrol ulang jika panas berlanjut lebih dari 3 hari.");
            insert(rm);
        }
    }

    public synchronized String generateNextId() {
        counter++;
        return String.format("RM-%03d", counter);
    }

    private void updateCounterFromId(String id) {
        if (id != null && id.startsWith("RM-")) {
            try {
                int val = Integer.parseInt(id.substring(3));
                if (val > counter) counter = val;
            } catch (NumberFormatException ignored) {}
        }
    }

    @Override
    public synchronized void insert(RekamMedis entity) {
        if (entity.getId() == null || entity.getId().trim().isEmpty()) {
            entity.setId(generateNextId());
        } else {
            updateCounterFromId(entity.getId());
        }
        storage.put(entity.getId(), entity);
        saveToFile();
    }

    @Override
    public synchronized void update(RekamMedis entity) {
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
    public synchronized Optional<RekamMedis> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public synchronized List<RekamMedis> findAll() {
        return new ArrayList<>(storage.values());
    }

    public synchronized Optional<RekamMedis> findByPemeriksaanId(String idPemeriksaan) {
        if (idPemeriksaan == null) return Optional.empty();
        return storage.values().stream()
                .filter(rm -> rm.getPemeriksaan() != null && idPemeriksaan.equalsIgnoreCase(rm.getPemeriksaan().getId()))
                .findFirst();
    }

    public synchronized List<RekamMedis> findByPasienId(String idPasien) {
        if (idPasien == null) return Collections.emptyList();
        return storage.values().stream()
                .filter(rm -> rm.getPasien() != null && idPasien.equalsIgnoreCase(rm.getPasien().getId()))
                .collect(Collectors.toList());
    }

    private synchronized void saveToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_PATH))) {
            for (RekamMedis rm : storage.values()) {
                String line = String.join("|",
                        rm.getId() != null ? rm.getId() : "",
                        (rm.getPemeriksaan() != null && rm.getPemeriksaan().getId() != null) ? rm.getPemeriksaan().getId() : "",
                        (rm.getPasien() != null && rm.getPasien().getId() != null) ? rm.getPasien().getId() : "",
                        (rm.getDokter() != null && rm.getDokter().getId() != null) ? rm.getDokter().getId() : "",
                        rm.getTanggal() != null ? rm.getTanggal().toString() : "",
                        escape(rm.getDiagnosis()),
                        escape(rm.getTindakan()),
                        escape(rm.getResepObat()),
                        escape(rm.getCatatan())
                );
                writer.println(line);
            }
        } catch (IOException e) {
            System.err.println("Gagal menyimpan rekam medis: " + e.getMessage());
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
                    String idPemeriksaan = parts[1];
                    String idPasien = parts[2];
                    String idDokter = parts[3];
                    LocalDate tgl = parts[4].isEmpty() ? LocalDate.now() : LocalDate.parse(parts[4]);
                    String diagnosis = unescape(parts[5]);
                    String tindakan = unescape(parts[6]);
                    String resep = unescape(parts[7]);
                    String catatan = unescape(parts[8]);

                    Pemeriksaan pemeriksaan = pemeriksaanDAO.findById(idPemeriksaan).orElse(null);
                    Pasien pasien = pasienDAO.findById(idPasien).orElse(null);
                    Dokter dokter = dokterDAO.findById(idDokter).orElse(null);

                    RekamMedis rm = new RekamMedis(id, pemeriksaan, pasien, dokter, tgl, diagnosis, tindakan, resep, catatan);
                    storage.put(id, rm);
                    updateCounterFromId(id);
                }
            }
        } catch (Exception e) {
            System.err.println("Gagal memuat rekam medis: " + e.getMessage());
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


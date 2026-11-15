package com.klinik.dao;

import com.klinik.exception.DataNotFoundException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.model.Dokter;
import com.klinik.model.Layanan;
import com.klinik.model.Pasien;
import com.klinik.model.Pemeriksaan;
import com.klinik.model.StatusPemeriksaan;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Data Access Object untuk entitas Pemeriksaan dengan persistensi file teks/CSV (data/pemeriksaan.txt).
 */
public class PemeriksaanDAO implements GenericDAO<Pemeriksaan, String> {
    private static final String DATA_DIR = "data";
    private static final String FILE_PATH = DATA_DIR + File.separator + "pemeriksaan.txt";
    private final Map<String, Pemeriksaan> storage = new LinkedHashMap<>();
    private final PasienDAO pasienDAO;
    private final DokterDAO dokterDAO;
    private final LayananDAO layananDAO;
    private int counter = 0;

    public PemeriksaanDAO(PasienDAO pasienDAO, DokterDAO dokterDAO, LayananDAO layananDAO) {
        this.pasienDAO = pasienDAO;
        this.dokterDAO = dokterDAO;
        this.layananDAO = layananDAO;
        initStorage();
    }

    private void initStorage() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) dir.mkdirs();
        File file = new File(FILE_PATH);
        if (file.exists()) {
            loadFromFile();
            ensureSampleActiveVisitsForDokter();
        } else {
            seedInitialData();
            saveToFile();
        }
    }

    private void seedInitialData() {
        List<Pasien> pasiens = pasienDAO.findAll();
        List<Dokter> dokters = dokterDAO.findAll();
        List<Layanan> layanans = layananDAO.findAll();

        if (!pasiens.isEmpty() && !dokters.isEmpty() && !layanans.isEmpty()) {
            Pemeriksaan p1 = new Pemeriksaan(generateNextId(), "P-001", pasiens.get(0), dokters.get(0), layanans.get(0),
                    LocalDate.now(), "Demam dan batuk pilek", "petugas01", StatusPemeriksaan.SELESAI, 1, "");
            insert(p1);

            if (pasiens.size() > 1 && dokters.size() > 1 && layanans.size() > 2) {
                Pemeriksaan p2 = new Pemeriksaan(generateNextId(), "P-002", pasiens.get(1), dokters.get(1), layanans.get(2),
                        LocalDate.now(), "Nyeri lambung kronis", "petugas01", StatusPemeriksaan.DIPERIKSA, 1, "");
                insert(p2);
            }

            if (pasiens.size() > 2) {
                Pemeriksaan p3 = new Pemeriksaan(generateNextId(), "P-003", pasiens.get(2), dokters.get(0), layanans.get(0),
                        LocalDate.now(), "Pusing dan lemas sejak 2 hari", "petugas01", StatusPemeriksaan.TTV_SELESAI, 1, "");
                insert(p3);
            }
        }
    }

    /**
     * Memastikan terdapat minimal satu pemeriksaan berstatus TTV_SELESAI / DIPERIKSA untuk DKT-001
     * agar fitur pembuatan surat oleh dokter01 (DKT-001) dan tampilan antrean dapat langsung diuji.
     */
    private void ensureSampleActiveVisitsForDokter() {
        boolean hasEligibleForDkt001 = storage.values().stream()
                .anyMatch(p -> p.getDokter() != null
                        && "DKT-001".equalsIgnoreCase(p.getDokter().getId())
                        && (p.getStatus() == StatusPemeriksaan.TTV_SELESAI || p.getStatus() == StatusPemeriksaan.DIPERIKSA));
        if (!hasEligibleForDkt001) {
            List<Pasien> pasiens = pasienDAO.findAll();
            Optional<Dokter> dkt001 = dokterDAO.findById("DKT-001");
            List<Layanan> layanans = layananDAO.findAll();
            if (!pasiens.isEmpty() && dkt001.isPresent() && !layanans.isEmpty()) {
                Pasien targetPasien = pasiens.size() > 2 ? pasiens.get(2) : pasiens.get(0);
                Pemeriksaan p = new Pemeriksaan(generateNextId(), "P-004", targetPasien, dkt001.get(), layanans.get(0),
                        LocalDate.now(), "Demam tinggi dan radang tenggorokan", "petugas01",
                        StatusPemeriksaan.TTV_SELESAI, 1, "");
                storage.put(p.getId(), p);
                saveToFile();
            }
        }
    }

    public synchronized String generateNextId() {
        counter++;
        return String.format("PMR-%03d", counter);
    }

    private void updateCounterFromId(String id) {
        if (id != null && (id.startsWith("PMR-") || id.startsWith("PMK-"))) {
            try {
                int val = Integer.parseInt(id.substring(4));
                if (val > counter) counter = val;
            } catch (NumberFormatException ignored) {}
        }
    }

    @Override
    public synchronized void insert(Pemeriksaan entity) {
        if (entity.getId() == null || entity.getId().trim().isEmpty()) {
            entity.setId(generateNextId());
        } else {
            updateCounterFromId(entity.getId());
        }
        storage.put(entity.getId(), entity);
        saveToFile();
    }

    @Override
    public synchronized void update(Pemeriksaan entity) {
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
    public synchronized Optional<Pemeriksaan> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public synchronized List<Pemeriksaan> findAll() {
        return new ArrayList<>(storage.values());
    }

    public synchronized boolean existsByDokterId(String dokterId) {
        if (dokterId == null) return false;
        return storage.values().stream()
                .anyMatch(p -> p.getDokter() != null && dokterId.equalsIgnoreCase(p.getDokter().getId()));
    }

    public synchronized boolean existsByPasienId(String pasienId) {
        if (pasienId == null) return false;
        return storage.values().stream()
                .anyMatch(p -> p.getPasien() != null && pasienId.equalsIgnoreCase(p.getPasien().getId()));
    }

    public synchronized List<Pemeriksaan> findByStatus(StatusPemeriksaan status) {
        return storage.values().stream()
                .filter(p -> p.getStatus() == status)
                .collect(Collectors.toList());
    }

    public synchronized List<Pemeriksaan> findByDokterId(String idDokter) {
        if (idDokter == null || idDokter.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return storage.values().stream()
                .filter(p -> p.getDokter() != null && idDokter.trim().equalsIgnoreCase(p.getDokter().getId()))
                .collect(Collectors.toList());
    }

    /**
     * BR-14: Memanggil pasien dalam antrean dan menambah hitungan jumlahPanggilan.
     */
    public synchronized Pemeriksaan panggilAntrean(String idPemeriksaan)
            throws DataNotFoundException, DataTidakValidException {
        Pemeriksaan p = findById(idPemeriksaan)
                .orElseThrow(() -> new DataNotFoundException("Data pemeriksaan " + idPemeriksaan + " tidak ditemukan."));
        if (p.getStatus() == StatusPemeriksaan.SELESAI || p.getStatus() == StatusPemeriksaan.DIBATALKAN) {
            throw new DataTidakValidException("Status Pemeriksaan",
                    "Antrean dengan status " + p.getStatus().getLabel() + " tidak dapat dipanggil lagi.");
        }
        p.tambahPanggilan();
        saveToFile();
        return p;
    }

    /**
     * BR-14 & BR-15: Membatalkan antrean pemeriksaan hanya jika jumlahPanggilan >= 3 dan alasan pembatalan diisi.
     */
    public synchronized Pemeriksaan batalkanAntrean(String idPemeriksaan, String alasanBatal)
            throws DataNotFoundException, DataTidakValidException {
        Pemeriksaan p = findById(idPemeriksaan)
                .orElseThrow(() -> new DataNotFoundException("Data pemeriksaan " + idPemeriksaan + " tidak ditemukan."));
        p.batalkan(alasanBatal);
        saveToFile();
        return p;
    }

    private synchronized void saveToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_PATH))) {
            for (Pemeriksaan p : storage.values()) {
                String line = String.join("|",
                        p.getId() != null ? p.getId() : "",
                        p.getNomorAntreanPoli() != null ? p.getNomorAntreanPoli() : "",
                        (p.getPasien() != null && p.getPasien().getId() != null) ? p.getPasien().getId() : "",
                        (p.getDokter() != null && p.getDokter().getId() != null) ? p.getDokter().getId() : "",
                        (p.getLayanan() != null && p.getLayanan().getId() != null) ? p.getLayanan().getId() : "",
                        p.getTanggalPemeriksaan() != null ? p.getTanggalPemeriksaan().toString() : "",
                        p.getKeluhanAwal() != null ? p.getKeluhanAwal().replace("\n", "\\n").replace("|", "/") : "",
                        p.getNamaPetugas() != null ? p.getNamaPetugas() : "",
                        p.getStatus() != null ? p.getStatus().name() : "",
                        String.valueOf(p.getJumlahPanggilan()),
                        p.getAlasanBatal() != null ? p.getAlasanBatal().replace("\n", "\\n").replace("|", "/") : ""
                );
                writer.println(line);
            }
        } catch (IOException e) {
            System.err.println("Gagal menyimpan pemeriksaan: " + e.getMessage());
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
                    String noAntrean = parts[1];
                    String idPasien = parts[2];
                    String idDokter = parts[3];
                    String idLayanan = parts[4];
                    LocalDate tgl = parts[5].isEmpty() ? LocalDate.now() : LocalDate.parse(parts[5]);
                    String keluhan = parts[6].replace("\\n", "\n");
                    String petugas = parts[7];
                    StatusPemeriksaan status = StatusPemeriksaan.fromString(parts[8]);
                    int jmlPanggilan = 0;
                    if (parts.length >= 10 && !parts[9].trim().isEmpty()) {
                        try {
                            jmlPanggilan = Integer.parseInt(parts[9].trim());
                        } catch (NumberFormatException ignored) {}
                    }
                    String alasanBatal = parts.length >= 11 ? parts[10].replace("\\n", "\n") : "";

                    Pasien pasien = pasienDAO.findById(idPasien).orElse(null);
                    Dokter dokter = dokterDAO.findById(idDokter).orElse(null);
                    Layanan layanan = layananDAO.findById(idLayanan).orElse(null);

                    Pemeriksaan p = new Pemeriksaan(id, noAntrean, pasien, dokter, layanan, tgl, keluhan, petugas, status, jmlPanggilan, alasanBatal);
                    storage.put(id, p);
                    updateCounterFromId(id);
                }
            }
        } catch (Exception e) {
            System.err.println("Gagal memuat pemeriksaan: " + e.getMessage());
        }
    }
}

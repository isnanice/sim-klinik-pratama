package com.klinik.dao;

import com.klinik.exception.DataNotFoundException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.model.JenisSurat;
import com.klinik.model.StatusSurat;
import com.klinik.model.Surat;

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
 * Data Access Object untuk entitas Surat (Surat Izin Sakit & Surat Rujukan)
 * dengan persistensi file (data/surat.txt).
 * Mendukung penyimpanan dengan status awal DITERUSKAN, perubahan status menjadi DISERAHKAN,
 * serta pemfilteran daftar surat.
 */
public class SuratDAO implements GenericDAO<Surat, String> {
    private static final String DEFAULT_DATA_DIR = "data";
    private final String dataDir;
    private final String filePath;

    private final Map<String, Surat> storage = new LinkedHashMap<>();
    private int counter = 0;

    public SuratDAO() {
        this(System.getProperty("klinik.data.dir", DEFAULT_DATA_DIR));
    }

    public SuratDAO(String dataDir) {
        this.dataDir = (dataDir != null && !dataDir.trim().isEmpty()) ? dataDir.trim() : DEFAULT_DATA_DIR;
        this.filePath = this.dataDir + File.separator + "surat.txt";
        initStorage();
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
        Surat s1 = new Surat(
                generateNextId(),
                "PMR-001",
                JenisSurat.IZIN,
                "Pasien mengalami ISPA dan membutuhkan istirahat medis di rumah.",
                3,
                "",
                LocalDate.now(),
                StatusSurat.DITERUSKAN,
                ""
        );
        storage.put(s1.getIdSurat(), s1);

        Surat s2 = new Surat(
                generateNextId(),
                "PMR-002",
                JenisSurat.RUJUKAN,
                "Mohon pemeriksaan endoskopi lanjutan atas indikasi gastritis kronis.",
                0,
                "RSUP Dr. Sardjito - Poli Gastroenterologi",
                LocalDate.now(),
                StatusSurat.DISERAHKAN,
                "Rudi Hartono"
        );
        storage.put(s2.getIdSurat(), s2);
    }

    public synchronized String generateNextId() {
        counter++;
        return String.format("SRT-%03d", counter);
    }

    private void updateCounterFromId(String id) {
        if (id != null && id.startsWith("SRT-")) {
            try {
                int val = Integer.parseInt(id.substring(4));
                if (val > counter) {
                    counter = val;
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    /**
     * Menyimpan surat baru dengan status awal DITERUSKAN.
     */
    public synchronized Surat simpan(Surat surat) throws DataTidakValidException {
        if (surat == null) {
            throw new DataTidakValidException("Surat", "Objek surat tidak boleh null.");
        }
        surat.validate();
        if (surat.getIdSurat() == null || surat.getIdSurat().trim().isEmpty()) {
            surat.setIdSurat(generateNextId());
        } else {
            updateCounterFromId(surat.getIdSurat());
        }
        if (surat.getTanggalDibuat() == null) {
            surat.setTanggalDibuat(LocalDate.now());
        }
        // Pastikan surat yang baru disimpan berstatus DITERUSKAN
        if (surat.getStatus() != StatusSurat.DISERAHKAN) {
            surat.setStatus(StatusSurat.DITERUSKAN);
        }
        storage.put(surat.getIdSurat(), surat);
        saveToFile();
        return surat;
    }

    @Override
    public synchronized void insert(Surat entity) {
        simpan(entity);
    }

    @Override
    public synchronized void update(Surat entity) throws DataTidakValidException, DataNotFoundException {
        if (entity == null || entity.getIdSurat() == null) {
            throw new DataTidakValidException("Surat", "ID Surat yang akan diperbarui tidak valid.");
        }
        Surat existing = storage.get(entity.getIdSurat());
        if (existing == null) {
            throw new DataNotFoundException("Surat dengan ID " + entity.getIdSurat() + " tidak ditemukan.");
        }
        if (existing.getStatus() == StatusSurat.DISERAHKAN) {
            throw new DataTidakValidException("Status Surat",
                    "Surat " + existing.getIdSurat() + " sudah berstatus DISERAHKAN dan tidak dapat diubah.");
        }
        entity.validate();
        storage.put(entity.getIdSurat(), entity);
        saveToFile();
    }

    @Override
    public synchronized void delete(String id) throws DataTidakValidException {
        if (id == null) {
            return;
        }
        Surat existing = storage.get(id);
        if (existing != null && existing.getStatus() == StatusSurat.DISERAHKAN) {
            throw new DataTidakValidException("Status Surat",
                    "Surat " + id + " sudah berstatus DISERAHKAN dan tidak dapat dihapus.");
        }
        storage.remove(id);
        saveToFile();
    }

    /**
     * Mengubah status surat dari DITERUSKAN menjadi DISERAHKAN serta mencatat petugas yang menyerahkan.
     * Surat yang sudah DISERAHKAN bersifat final dan tidak dapat diubah kembali.
     */
    public synchronized Surat ubahStatusDiserahkan(String idSurat, String diserahkanOleh)
            throws DataNotFoundException, DataTidakValidException {
        if (idSurat == null || idSurat.trim().isEmpty()) {
            throw new DataTidakValidException("ID Surat", "ID Surat wajib dipilih.");
        }
        Surat surat = storage.get(idSurat.trim());
        if (surat == null) {
            throw new DataNotFoundException("Surat dengan ID " + idSurat + " tidak ditemukan.");
        }
        if (surat.getStatus() == StatusSurat.DISERAHKAN) {
            throw new DataTidakValidException("Status Surat",
                    "Surat " + idSurat + " sudah diserahkan sebelumnya oleh " + surat.getDiserahkanOleh() + ".");
        }
        surat.tandaiDiserahkan(diserahkanOleh);
        saveToFile();
        return surat;
    }

    @Override
    public synchronized Optional<Surat> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public synchronized List<Surat> findAll() {
        return new ArrayList<>(storage.values());
    }

    public synchronized List<Surat> findByStatus(StatusSurat status) {
        if (status == null) {
            return findAll();
        }
        return storage.values().stream()
                .filter(s -> s.getStatus() == status)
                .collect(Collectors.toList());
    }

    public synchronized List<Surat> findByJenis(JenisSurat jenis) {
        if (jenis == null) {
            return findAll();
        }
        return storage.values().stream()
                .filter(s -> s.getJenis() == jenis)
                .collect(Collectors.toList());
    }

    public synchronized List<Surat> findByPemeriksaanId(String idPemeriksaan) {
        if (idPemeriksaan == null || idPemeriksaan.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String clean = idPemeriksaan.trim();
        return storage.values().stream()
                .filter(s -> s.getIdPemeriksaan() != null && s.getIdPemeriksaan().equalsIgnoreCase(clean))
                .collect(Collectors.toList());
    }

    /**
     * Memfilter daftar surat berdasarkan jenis, status, dan kata kunci pencarian.
     */
    public synchronized List<Surat> filterSurat(JenisSurat jenis, StatusSurat status, String keyword) {
        String kw = keyword != null ? keyword.trim().toLowerCase() : "";
        return storage.values().stream()
                .filter(s -> jenis == null || s.getJenis() == jenis)
                .filter(s -> status == null || s.getStatus() == status)
                .filter(s -> {
                    if (kw.isEmpty()) return true;
                    return (s.getIdSurat() != null && s.getIdSurat().toLowerCase().contains(kw)) ||
                           (s.getIdPemeriksaan() != null && s.getIdPemeriksaan().toLowerCase().contains(kw)) ||
                           (s.getIsi() != null && s.getIsi().toLowerCase().contains(kw)) ||
                           (s.getTujuanRujukan() != null && s.getTujuanRujukan().toLowerCase().contains(kw)) ||
                           (s.getDiserahkanOleh() != null && s.getDiserahkanOleh().toLowerCase().contains(kw));
                })
                .collect(Collectors.toList());
    }

    private synchronized void saveToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            for (Surat s : storage.values()) {
                // Format: idSurat|idPemeriksaan|jenis|isi|jumlahHari|tujuanRujukan|tanggalDibuat|status|diserahkanOleh
                String line = String.join("|",
                        escape(s.getIdSurat()),
                        escape(s.getIdPemeriksaan()),
                        s.getJenis() != null ? s.getJenis().name() : "",
                        escape(s.getIsi()),
                        String.valueOf(s.getJumlahHari()),
                        escape(s.getTujuanRujukan()),
                        s.getTanggalDibuat() != null ? s.getTanggalDibuat().toString() : "",
                        s.getStatus() != null ? s.getStatus().name() : StatusSurat.DITERUSKAN.name(),
                        escape(s.getDiserahkanOleh())
                );
                writer.println(line);
            }
        } catch (IOException e) {
            System.err.println("Gagal menyimpan data surat: " + e.getMessage());
        }
    }

    private synchronized void loadFromFile() {
        storage.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length >= 9) {
                    String idSurat = unescape(parts[0]);
                    String idPemeriksaan = unescape(parts[1]);
                    JenisSurat jenis = JenisSurat.fromString(parts[2]);
                    String isi = unescape(parts[3]);
                    int jumlahHari = 0;
                    try {
                        jumlahHari = Integer.parseInt(parts[4].trim());
                    } catch (NumberFormatException ignored) {
                    }
                    String tujuanRujukan = unescape(parts[5]);
                    LocalDate tglDibuat = parts[6].isEmpty() ? LocalDate.now() : LocalDate.parse(parts[6]);
                    StatusSurat status = StatusSurat.fromString(parts[7]);
                    String diserahkanOleh = unescape(parts[8]);

                    Surat surat = new Surat(idSurat, idPemeriksaan, jenis, isi,
                            jumlahHari, tujuanRujukan, tglDibuat, status, diserahkanOleh);
                    storage.put(idSurat, surat);
                    updateCounterFromId(idSurat);
                }
            }
        } catch (Exception e) {
            System.err.println("Gagal memuat data surat: " + e.getMessage());
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

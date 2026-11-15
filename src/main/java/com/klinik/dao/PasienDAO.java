package com.klinik.dao;

import com.klinik.exception.DataMasihDipakaiException;
import com.klinik.exception.DataNotFoundException;
import com.klinik.model.JenisKelamin;
import com.klinik.model.Pasien;

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
 * Data Access Object untuk entitas Pasien dengan persistensi file teks/CSV (data/pasien.txt).
 * Mengimplementasikan GenericDAO serta mendukung pencarian NIK/ID/Nama, nonaktifkan pasien,
 * dan proteksi hapus permanen bila pasien memiliki riwayat pemeriksaan.
 */
public class PasienDAO implements GenericDAO<Pasien, String> {
    private static final String DATA_DIR = "data";
    private static final String FILE_PATH = DATA_DIR + File.separator + "pasien.txt";
    private static final String PEMERIKSAAN_FILE_PATH = DATA_DIR + File.separator + "pemeriksaan.txt";

    private final Map<String, Pasien> storage = new LinkedHashMap<>();
    private PemeriksaanDAO pemeriksaanDAO;
    private int counter = 0;

    public PasienDAO() {
        initStorage();
    }

    public PasienDAO(PemeriksaanDAO pemeriksaanDAO) {
        this.pemeriksaanDAO = pemeriksaanDAO;
        initStorage();
    }

    public void setPemeriksaanDAO(PemeriksaanDAO pemeriksaanDAO) {
        this.pemeriksaanDAO = pemeriksaanDAO;
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
        insert(new Pasien(generateNextId(), "3404011205900001", "Budi Santoso", JenisKelamin.LAKI_LAKI, LocalDate.of(1990, 5, 12),
                "Jl. Malioboro No. 12, Yogyakarta", "081234567890", "0001234567890", LocalDate.now(), true));
        insert(new Pasien(generateNextId(), "3404024309850002", "Sari Dewi", JenisKelamin.PEREMPUAN, LocalDate.of(1985, 9, 3),
                "Jl. Gejayan No. 44, Sleman", "081298765432", "0001234567891", LocalDate.now(), true));
        insert(new Pasien(generateNextId(), "3404032101010003", "Agus Prasetyo", JenisKelamin.LAKI_LAKI, LocalDate.of(2001, 1, 21),
                "Jl. Palagan No. 8, Sleman", "085711223344", "0001234567892", LocalDate.now(), true));
        insert(new Pasien(generateNextId(), "3471045407780004", "Rina Wulandari", JenisKelamin.PEREMPUAN, LocalDate.of(1978, 7, 14),
                "Jl. Kusumanegara No. 99, Yogyakarta", "082133445566", "0001234567893", LocalDate.now(), false));
        insert(new Pasien(generateNextId(), "3402053011950005", "Dimas Aditya", JenisKelamin.LAKI_LAKI, LocalDate.of(1995, 11, 30),
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
                if (val > counter) {
                    counter = val;
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @Override
    public synchronized void insert(Pasien pasien) {
        if (pasien == null) {
            return;
        }
        if (pasien.getId() == null || pasien.getId().trim().isEmpty()) {
            pasien.setId(generateNextId());
        } else {
            updateCounterFromId(pasien.getId());
        }
        storage.put(pasien.getId(), pasien);
        saveToFile();
    }

    public synchronized void simpan(Pasien pasien) {
        insert(pasien);
    }

    @Override
    public synchronized void update(Pasien pasien) {
        if (pasien != null && pasien.getId() != null) {
            storage.put(pasien.getId(), pasien);
            saveToFile();
        }
    }

    public synchronized void ubah(Pasien pasien) {
        update(pasien);
    }

    /**
     * Menghapus pasien secara permanen.
     * Melempar DataMasihDipakaiException apabila pasien telah memiliki riwayat pemeriksaan.
     */
    @Override
    public synchronized void delete(String id) throws DataMasihDipakaiException {
        if (id == null) {
            return;
        }
        Pasien pasien = storage.get(id);
        if (hasRiwayatPemeriksaan(id)) {
            String nama = pasien != null ? pasien.getNama() : id;
            throw new DataMasihDipakaiException("Pasien", id,
                    "Pasien " + nama + " (" + id + ") memiliki riwayat pemeriksaan dan tidak dapat dihapus permanen. Gunakan fitur Nonaktifkan.");
        }
        storage.remove(id);
        saveToFile();
    }

    public synchronized void hapus(String id) throws DataMasihDipakaiException {
        delete(id);
    }

    /**
     * Mengecek apakah pasien dengan ID tertentu memiliki riwayat pemeriksaan,
     * baik melalui referensi PemeriksaanDAO maupun file persistensi pemeriksaan.txt.
     */
    public synchronized boolean hasRiwayatPemeriksaan(String pasienId) {
        if (pasienId == null || pasienId.trim().isEmpty()) {
            return false;
        }
        if (pemeriksaanDAO != null && pemeriksaanDAO.existsByPasienId(pasienId)) {
            return true;
        }
        File file = new File(PEMERIKSAAN_FILE_PATH);
        if (!file.exists()) {
            return false;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length >= 3 && pasienId.trim().equalsIgnoreCase(parts[2].trim())) {
                    return true;
                }
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    /**
     * Mengubah status aktif/nonaktif pasien.
     */
    public synchronized void toggleAktif(String id, boolean aktif) {
        Pasien pasien = storage.get(id);
        if (pasien != null) {
            pasien.setAktif(aktif);
            saveToFile();
        }
    }

    /**
     * Menonaktifkan pasien (soft-delete) berdasarkan ID.
     */
    public synchronized void nonaktifkanPasien(String id) throws DataNotFoundException {
        Pasien pasien = storage.get(id);
        if (pasien == null) {
            throw new DataNotFoundException("Pasien dengan ID " + id + " tidak ditemukan.");
        }
        pasien.setAktif(false);
        saveToFile();
    }

    @Override
    public synchronized Optional<Pasien> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(storage.get(id));
    }

    public synchronized Optional<Pasien> findByNik(String nik) {
        if (nik == null || nik.trim().isEmpty()) {
            return Optional.empty();
        }
        String clean = nik.trim();
        return storage.values().stream()
                .filter(p -> p.getNik() != null && p.getNik().equalsIgnoreCase(clean))
                .findFirst();
    }

    public synchronized List<Pasien> findByNama(String nama) {
        if (nama == null || nama.trim().isEmpty()) {
            return findAll();
        }
        String q = nama.trim().toLowerCase();
        return storage.values().stream()
                .filter(p -> p.getNama() != null && p.getNama().toLowerCase().contains(q))
                .collect(Collectors.toList());
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

    /**
     * Pencarian pasien berdasarkan kata kunci dan kriteria ("Semua", "ID", "NIK", "Nama", "No. BPJS").
     */
    public synchronized List<Pasien> search(String keyword, String criteria) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        String kw = keyword.trim().toLowerCase();
        String krit = criteria == null ? "Semua" : criteria.trim();
        return storage.values().stream()
                .filter(p -> {
                    boolean matchId = p.getId() != null && p.getId().toLowerCase().contains(kw);
                    boolean matchNik = p.getNik() != null && p.getNik().toLowerCase().contains(kw);
                    boolean matchNama = p.getNama() != null && p.getNama().toLowerCase().contains(kw);
                    boolean matchBpjs = p.getNoBpjs() != null && p.getNoBpjs().toLowerCase().contains(kw);

                    if ("ID".equalsIgnoreCase(krit)) {
                        return matchId;
                    } else if ("NIK".equalsIgnoreCase(krit)) {
                        return matchNik;
                    } else if ("Nama".equalsIgnoreCase(krit)) {
                        return matchNama;
                    } else if ("No. BPJS".equalsIgnoreCase(krit) || "BPJS".equalsIgnoreCase(krit)) {
                        return matchBpjs;
                    } else {
                        return matchId || matchNik || matchNama || matchBpjs;
                    }
                })
                .collect(Collectors.toList());
    }

    public synchronized List<Pasien> cari(String keyword) {
        return search(keyword, "Semua");
    }

    private synchronized void saveToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_PATH))) {
            for (Pasien p : storage.values()) {
                // Format 10 kolom: id|nik|nama|jenisKelamin|tanggalLahir|alamat|noTelepon|noBpjs|tanggalRegistrasi|aktif
                String line = String.join("|",
                        escape(p.getId()),
                        escape(p.getNik()),
                        escape(p.getNama()),
                        p.getJenisKelamin() != null ? p.getJenisKelamin().name() : "",
                        p.getTanggalLahir() != null ? p.getTanggalLahir().toString() : "",
                        escape(p.getAlamat()),
                        escape(p.getNoTelepon()),
                        escape(p.getNoBpjs()),
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
                if (parts.length >= 10) {
                    String id = unescape(parts[0]);
                    String nik = unescape(parts[1]);
                    String nama = unescape(parts[2]);
                    JenisKelamin jk = JenisKelamin.fromString(parts[3]);
                    LocalDate tglLahir = parts[4].isEmpty() ? null : LocalDate.parse(parts[4]);
                    String alamat = unescape(parts[5]);
                    String noTelp = unescape(parts[6]);
                    String noBpjs = unescape(parts[7]);
                    LocalDate tglReg = parts[8].isEmpty() ? LocalDate.now() : LocalDate.parse(parts[8]);
                    boolean aktif = Boolean.parseBoolean(parts[9]);

                    Pasien p = new Pasien(id, nik, nama, jk, tglLahir, alamat, noTelp, noBpjs, tglReg, aktif);
                    storage.put(id, p);
                    updateCounterFromId(id);
                } else if (parts.length == 9) {
                    // Kompatibilitas format 9 kolom lama (tanpa kolom NIK terpisah)
                    String id = unescape(parts[0]);
                    String nama = unescape(parts[1]);
                    JenisKelamin jk = JenisKelamin.fromString(parts[2]);
                    LocalDate tglLahir = parts[3].isEmpty() ? null : LocalDate.parse(parts[3]);
                    String alamat = unescape(parts[4]);
                    String noTelp = unescape(parts[5]);
                    String noBpjs = unescape(parts[6]);
                    LocalDate tglReg = parts[7].isEmpty() ? LocalDate.now() : LocalDate.parse(parts[7]);
                    boolean aktif = Boolean.parseBoolean(parts[8]);

                    Pasien p = new Pasien(id, "", nama, jk, tglLahir, alamat, noTelp, noBpjs, tglReg, aktif);
                    storage.put(id, p);
                    updateCounterFromId(id);
                }
            }
        } catch (Exception e) {
            System.err.println("Gagal memuat pasien: " + e.getMessage());
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

package com.klinik.util;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Pembuat koneksi PostgreSQL. Setiap pemanggilan getConnection() menghasilkan koneksi baru,
 * jadi DAO memakainya dengan try-with-resources dan koneksi tertutup otomatis.
 *
 * Urutan sumber konfigurasi:
 *   1. Variabel lingkungan KLINIK_DB_URL, KLINIK_DB_USER, KLINIK_DB_PASSWORD
 *   2. Berkas db.properties di folder proyek (atau jalur lain lewat -Dklinik.config=...)
 */
public final class DatabaseConnection {

    private static final String BERKAS_DEFAULT = "db.properties";
    private static Properties berkas;   // dimuat sekali
    private static boolean driverLoaded = false;

    static {
        try {
            Class.forName("org.postgresql.Driver");
            driverLoaded = true;
        } catch (ClassNotFoundException e) {
            System.err.println("[Peringatan] Driver org.postgresql.Driver tidak ditemukan di classpath.");
        }
    }

    private DatabaseConnection() { }

    public static Connection getConnection() throws SQLException {
        if (!driverLoaded) {
            try {
                Class.forName("org.postgresql.Driver");
                driverLoaded = true;
            } catch (ClassNotFoundException e) {
                throw new SQLException("Driver PostgreSQL (postgresql-42.7.4.jar) tidak ditemukan di classpath!\n"
                        + "Solusi di Eclipse: Klik kanan proyek -> Build Path -> Configure Build Path -> Libraries -> Add JARs -> pilih lib/postgresql-42.7.4.jar, atau jalankan via run.bat.");
            }
        }

        String url      = ambil("KLINIK_DB_URL",      "db.url");
        String user     = ambil("KLINIK_DB_USER",     "db.user");
        String password = ambil("KLINIK_DB_PASSWORD", "db.password");

        // Normalisasi URL jika ada spasi tidak sengaja (misal 'jdbc: postgresql://')
        url = url.trim().replaceAll("^jdbc:\\s+", "jdbc:");

        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new SQLException("Koneksi database gagal: " + e.getMessage() + "\nURL: " + url + "\nUser: " + user, e);
        }
    }

    private static String ambil(String namaEnv, String kunciBerkas) throws SQLException {
        String nilai = System.getenv(namaEnv);
        if (nilai == null || nilai.isBlank()) {
            nilai = muatBerkas().getProperty(kunciBerkas);
        }
        if (nilai == null || nilai.isBlank()) {
            throw new SQLException("Konfigurasi database belum lengkap: isi '" + kunciBerkas
                    + "' di db.properties atau set variabel lingkungan " + namaEnv + ".");
        }
        return nilai.trim();
    }

    private static synchronized Properties muatBerkas() throws SQLException {
        if (berkas == null) {
            Properties p = new Properties();
            Path jalur = Path.of(System.getProperty("klinik.config", BERKAS_DEFAULT));
            if (Files.exists(jalur)) {
                try (Reader r = Files.newBufferedReader(jalur, StandardCharsets.UTF_8)) {
                    p.load(r);
                } catch (IOException e) {
                    throw new SQLException("Gagal membaca " + jalur.toAbsolutePath() + ": " + e.getMessage(), e);
                }
            } else {
                // Fallback default jika db.properties belum ada
                p.setProperty("db.url", "jdbc:postgresql://localhost:5432/db_klinik_pratama");
                p.setProperty("db.user", "klinik_user");
                p.setProperty("db.password", "klinik123");
            }
            berkas = p;
        }
        return berkas;
    }
}

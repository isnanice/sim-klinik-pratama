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
 * Urutan sumber konfigurasi (yang pertama ditemukan dipakai):
 *   1. Variabel lingkungan KLINIK_DB_URL, KLINIK_DB_USER, KLINIK_DB_PASSWORD
 *   2. Berkas db.properties di folder proyek (atau jalur lain lewat -Dklinik.config=...)
 */
public final class DatabaseConnection {

    private static final String BERKAS_DEFAULT = "db.properties";
    private static Properties berkas;   // dimuat sekali

    private DatabaseConnection() { }

    public static Connection getConnection() throws SQLException {
        String url      = ambil("KLINIK_DB_URL",      "db.url");
        String user     = ambil("KLINIK_DB_USER",     "db.user");
        String password = ambil("KLINIK_DB_PASSWORD", "db.password");
        return DriverManager.getConnection(url, user, password);
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
            }
            berkas = p;
        }
        return berkas;
    }
}
package com.klinik.util;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Pembuat koneksi PostgreSQL. Setiap pemanggilan getConnection() menghasilkan koneksi baru,
 * jadi DAO memakainya dengan try-with-resources dan koneksi tertutup otomatis.
 *
 * Urutan sumber konfigurasi (yang pertama ditemukan dipakai):
 *   1. Variabel lingkungan KLINIK_DB_URL, KLINIK_DB_USER, KLINIK_DB_PASSWORD
 *   2. Berkas db.properties di folder proyek (atau jalur lain lewat -Dklinik.config=...)
 */
public final class DatabaseConnection {

    private static final String DRIVER_CLASS = "org.postgresql.Driver";
    private static final String BERKAS_DEFAULT = "db.properties";
    private static Properties berkas;   // dimuat sekali
    private static volatile boolean driverTerdaftar = false;

    private DatabaseConnection() { }

    public static Connection getConnection() throws SQLException {
        pastikanDriverTerdaftar();
        String url      = ambil("KLINIK_DB_URL",      "db.url");
        String user     = ambil("KLINIK_DB_USER",     "db.user");
        String password = ambil("KLINIK_DB_PASSWORD", "db.password");
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Memastikan driver JDBC PostgreSQL (org.postgresql.Driver) terdaftar di DriverManager.
     * Memanggil Class.forName("org.postgresql.Driver") secara eksplisit, serta memiliki fallback
     * pemuatan otomatis dari repositori lokal .m2 apabila aplikasi dijalankan dari IDE tanpa classpath Maven.
     */
    private static synchronized void pastikanDriverTerdaftar() throws SQLException {
        if (driverTerdaftar) {
            return;
        }
        try {
            Class.forName(DRIVER_CLASS);
            driverTerdaftar = true;
            return;
        } catch (ClassNotFoundException ignored) {
            // Coba muat otomatis dari repositori Maven lokal (~/.m2) bila IDE belum memuat classpath pom.xml
        }

        Path jarPath = Path.of(System.getProperty("user.home"),
                ".m2", "repository", "org", "postgresql", "postgresql", "42.7.3", "postgresql-42.7.3.jar");
        if (Files.exists(jarPath)) {
            try {
                URL jarUrl = jarPath.toUri().toURL();
                URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl}, DatabaseConnection.class.getClassLoader());
                Class<?> clazz = Class.forName(DRIVER_CLASS, true, loader);
                Driver driver = (Driver) clazz.getDeclaredConstructor().newInstance();
                DriverManager.registerDriver(new DriverShim(driver));
                driverTerdaftar = true;
                return;
            } catch (Exception e) {
                throw new SQLException("Gagal memuat driver PostgreSQL dari " + jarPath + ": " + e.getMessage(), e);
            }
        }

        throw new SQLException("Driver JDBC PostgreSQL (" + DRIVER_CLASS + ") tidak ditemukan di classpath maupun di "
                + jarPath.toAbsolutePath() + ". Jalankan 'mvn dependency:resolve' terlebih dahulu.");
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

    /**
     * Wrapper Driver agar DriverManager mengizinkan pemakaian driver yang dimuat lewat URLClassLoader.
     */
    private static final class DriverShim implements Driver {
        private final Driver delegate;

        DriverShim(Driver delegate) {
            this.delegate = delegate;
        }

        @Override
        public Connection connect(String u, Properties p) throws SQLException {
            return delegate.connect(u, p);
        }

        @Override
        public boolean acceptsURL(String u) throws SQLException {
            return delegate.acceptsURL(u);
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException {
            return delegate.getPropertyInfo(u, p);
        }

        @Override
        public int getMajorVersion() {
            return delegate.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return delegate.getMinorVersion();
        }

        @Override
        public boolean jdbcCompliant() {
            return delegate.jdbcCompliant();
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return delegate.getParentLogger();
        }
    }
}

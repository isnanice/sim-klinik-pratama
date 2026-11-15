package com.klinik.test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import com.klinik.util.DatabaseConnection;

/** Uji koneksi: jalankan sekali untuk memastikan konfigurasi, driver, dan schema/seed sudah benar. */
public class KoneksiTest {

    private static final String[] TABEL = {
        "petugas", "dokter", "pasien", "layanan", "pemeriksaan", "rekam_medis", "pembayaran", "surat"
    };

    public static void main(String[] args) {
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement()) {
            System.out.println("Terhubung ke : " + c.getMetaData().getURL());
            System.out.println("Versi server : " + c.getMetaData().getDatabaseProductVersion());
            for (String tabel : TABEL) {
                try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + tabel)) {
                    rs.next();
                    System.out.printf("  %-12s %d baris%n", tabel, rs.getInt(1));
                }
            }
            System.out.println("Koneksi dan schema OK.");
        } catch (SQLException e) {
            System.err.println("GAGAL: " + e.getMessage());
            System.exit(1);
        }
    }
}
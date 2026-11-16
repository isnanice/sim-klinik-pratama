package com.klinik.util;

import com.klinik.exception.DataMasihDipakaiException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.exception.KlinikException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Utilitas helper JDBC untuk pengambilan ID sequence melalui fungsi PostgreSQL fn_id,
 * normalisasi parameter NULL, dan konversi SQLException ke KlinikException menggunakan PesanDb.
 */
public final class Jdbc {

    private Jdbc() {
    }

    /**
     * Mengambil ID baru berformat PREFIX-xxx dari sequence PostgreSQL menggunakan fungsi fn_id(prefix, nextval(seq)).
     */
    public static String nextId(Connection conn, String prefix, String sequenceName) throws SQLException {
        String sql = "SELECT fn_id(?, nextval(?))";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefix);
            ps.setString(2, sequenceName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
                throw new SQLException("Gagal menghasilkan ID dari sequence " + sequenceName);
            }
        }
    }

    /**
     * Mengambil ID baru menggunakan koneksi baru dari DatabaseConnection.
     */
    public static String nextId(String prefix, String sequenceName) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return nextId(conn, prefix, sequenceName);
        }
    }

    /**
     * Mengubah string kosong atau whitespace menjadi null agar sesuai dengan CHECK constraint PostgreSQL.
     */
    public static String emptyToNull(String val) {
        return (val == null || val.trim().isEmpty()) ? null : val.trim();
    }

    /**
     * Mengecek apakah koneksi PostgreSQL dapat dibuka dengan konfigurasi saat ini.
     */
    public static boolean isDatabaseAvailable() {
        try (Connection c = DatabaseConnection.getConnection()) {
            return c != null && !c.isClosed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Memetakan SQLException PostgreSQL (termasuk trigger BR-xx dan ON DELETE RESTRICT 23503) ke KlinikException.
     */
    public static KlinikException toKlinikException(SQLException e, String entityName, String entityId) {
        if (PesanDb.dataDipakai(e)) {
            return new DataMasihDipakaiException(entityName, entityId, PesanDb.untukPengguna(e));
        }
        if (PesanDb.duplikat(e) || PesanDb.tidakValid(e) || PesanDb.pelanggaranAturan(e)) {
            return new DataTidakValidException(entityName, PesanDb.untukPengguna(e));
        }
        return new KlinikException("Kesalahan database: " + PesanDb.untukPengguna(e), e);
    }
}

package com.klinik.dao;

import com.klinik.exception.DataMasihDipakaiException;
import com.klinik.exception.DatabaseException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.exception.KlinikException;
import com.klinik.util.DatabaseConnection;
import com.klinik.util.PesanDb;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;

/** Fungsi bantu JDBC yang dipakai bersama oleh semua DAO (satu tempat untuk pembuatan ID dan penerjemahan error). */
public final class Jdbc {
    private Jdbc() { }

    public static Connection buka() throws SQLException {
        return DatabaseConnection.getConnection();
    }

    /** ID berikutnya dari sequence PostgreSQL, misalnya PSN-007. Nama sequence berasal dari konstanta DAO, bukan input pengguna. */
    public static String idBerikut(String prefix, String sequence) {
        try (Connection c = buka()) {
            return idBerikut(c, prefix, sequence);
        } catch (SQLException e) {
            throw gagal(e);
        }
    }

    public static String idBerikut(Connection c, String prefix, String sequence) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT fn_id(?, nextval(?::regclass))")) {
            ps.setString(1, prefix);
            ps.setString(2, sequence);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }

    public static String nextId(String prefix, String sequence) throws SQLException {
        try (Connection c = buka()) {
            return idBerikut(c, prefix, sequence);
        }
    }

    public static String nextId(Connection c, String prefix, String sequence) throws SQLException {
        return idBerikut(c, prefix, sequence);
    }

    public static String emptyToNull(String val) {
        return (val == null || val.trim().isEmpty()) ? null : val.trim();
    }

    /** Memetakan SQLException PostgreSQL (termasuk trigger BR-xx dan ON DELETE RESTRICT 23503/23001) ke KlinikException. */
    public static KlinikException toKlinikException(SQLException e, String entityName, String entityId) {
        if (PesanDb.dataDipakai(e)) {
            return new DataMasihDipakaiException(entityName, entityId, PesanDb.untukPengguna(e));
        }
        return gagal(e);
    }

    /** Kesalahan input yang ditolak database menjadi DataTidakValidException; sisanya DatabaseException. */
    public static KlinikException gagal(SQLException e) {
        if (PesanDb.pelanggaranAturan(e) || PesanDb.tidakValid(e) || PesanDb.duplikat(e)) {
            return new DataTidakValidException(PesanDb.untukPengguna(e));
        }
        if ("23502".equals(e.getSQLState())) {   // NOT NULL
            String pesan = PesanDb.untukPengguna(e);
            int a = pesan.indexOf('"');
            int b = a >= 0 ? pesan.indexOf('"', a + 1) : -1;
            String kolom = (a >= 0 && b > a) ? pesan.substring(a + 1, b).replace('_', ' ') : "wajib";
            return new DataTidakValidException("Isian " + kolom + " wajib diisi.");
        }
        if (PesanDb.dataDipakai(e)) {
            return new DataTidakValidException("Data terkait tidak ditemukan atau masih dipakai data lain.");
        }
        String state = e.getSQLState();
        if (state == null || state.startsWith("08") || state.startsWith("28")) {
            return new DatabaseException("Koneksi ke database gagal. Pastikan PostgreSQL berjalan dan "
                    + "db.properties sudah benar. Detail: " + PesanDb.untukPengguna(e), e);
        }
        return new DatabaseException("Terjadi kesalahan database: " + PesanDb.untukPengguna(e), e);
    }

    static void teksAtauNull(PreparedStatement ps, int idx, String nilai) throws SQLException {
        if (nilai == null || nilai.trim().isEmpty()) {
            ps.setNull(idx, Types.VARCHAR);
        } else {
            ps.setString(idx, nilai.trim());
        }
    }

    static String teks(ResultSet rs, String kolom) throws SQLException {
        String v = rs.getString(kolom);
        return v != null ? v : "";
    }

    static LocalDate tanggal(ResultSet rs, String kolom) throws SQLException {
        Date d = rs.getDate(kolom);
        return d != null ? d.toLocalDate() : null;
    }

    static Integer intAtauNull(ResultSet rs, String kolom) throws SQLException {
        int v = rs.getInt(kolom);
        return rs.wasNull() ? null : v;
    }

    static Double doubleAtauNull(ResultSet rs, String kolom) throws SQLException {
        double v = rs.getDouble(kolom);
        return rs.wasNull() ? null : v;
    }

    static void intAtauNull(PreparedStatement ps, int idx, Integer v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.INTEGER); else ps.setInt(idx, v);
    }

    static void doubleAtauNull(PreparedStatement ps, int idx, Double v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.NUMERIC); else ps.setDouble(idx, v);
    }

    static String like(String kataKunci) {
        return "%" + kataKunci.trim().toLowerCase() + "%";
    }
}

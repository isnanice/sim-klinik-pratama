package com.klinik.util;

import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Menerjemahkan SQLException dari PostgreSQL menjadi pesan yang dipahami pengguna. */
public final class PesanDb {

    public static final String PELANGGARAN_ATURAN = "P0001";   // RAISE EXCEPTION dari trigger (BR-xx)
    public static final String DATA_DIPAKAI       = "23503";   // foreign_key_violation (data punya riwayat)
    public static final String RESTRICT_DIPAKAI   = "23001";   // restrict_violation pada ON DELETE RESTRICT (PostgreSQL 17/18+)
    public static final String DUPLIKAT           = "23505";   // unique
    public static final String TIDAK_VALID        = "23514";   // check

    private static final Pattern POLA_CONSTRAINT = Pattern.compile("constraint \"([^\"]+)\"");

    private static final Map<String, String> PESAN_CONSTRAINT = Map.ofEntries(
        Map.entry("chk_pasien_nik",      "NIK harus terdiri dari 16 digit angka."),
        Map.entry("chk_pasien_bpjs",     "Nomor BPJS harus 13 digit angka."),
        Map.entry("chk_pasien_nama",     "Nama tidak boleh kosong."),
        Map.entry("chk_pasien_telepon",  "Format nomor telepon tidak valid."),
        Map.entry("chk_pasien_lahir",    "Tanggal lahir tidak boleh di masa depan."),
        Map.entry("uq_pasien_nik",       "NIK sudah terdaftar."),
        Map.entry("chk_dokter_telepon",  "Format nomor telepon tidak valid."),
        Map.entry("uq_dokter_no_reg",    "Nomor registrasi dokter sudah terdaftar."),
        Map.entry("chk_layanan_tarif",   "Tarif tidak boleh negatif."),
        Map.entry("uq_petugas_username", "Username sudah dipakai."),
        Map.entry("chk_pmr_batal",       "Pembatalan wajib disertai alasan."),
        Map.entry("uq_byr_pemeriksaan",  "Kunjungan ini sudah memiliki pembayaran."),
        Map.entry("uq_rm_pemeriksaan",   "Kunjungan ini sudah memiliki rekam medis."),
        Map.entry("chk_srt_isian",       "Surat izin wajib jumlah hari minimal 1, surat rujukan wajib tujuan rujukan."),
        Map.entry("chk_rm_tensi",        "Tensi sistolik harus lebih besar dari diastolik."));

    private PesanDb() { }

    /** Pesan untuk pengguna. Pesan trigger (BR-xx) dipakai apa adanya, constraint dicarikan padanannya. */
    public static String untukPengguna(SQLException e) {
        String mentah = baris1(e);
        if (PELANGGARAN_ATURAN.equals(e.getSQLState())) {
            return mentah;
        }
        Matcher m = POLA_CONSTRAINT.matcher(mentah);
        if (m.find() && PESAN_CONSTRAINT.containsKey(m.group(1))) {
            return PESAN_CONSTRAINT.get(m.group(1));
        }
        if (dataDipakai(e)) {
            return "Data masih dipakai oleh data lain (memiliki riwayat) sehingga tidak dapat dihapus.";
        }
        return mentah;
    }

    public static boolean pelanggaranAturan(SQLException e) { return PELANGGARAN_ATURAN.equals(e.getSQLState()); }
    public static boolean dataDipakai(SQLException e)       { return DATA_DIPAKAI.equals(e.getSQLState()) || RESTRICT_DIPAKAI.equals(e.getSQLState()); }
    public static boolean duplikat(SQLException e)          { return DUPLIKAT.equals(e.getSQLState()); }
    public static boolean tidakValid(SQLException e)        { return TIDAK_VALID.equals(e.getSQLState()); }

    private static String baris1(SQLException e) {
        String pesan = e.getMessage() == null ? "" : e.getMessage();
        int akhir = pesan.indexOf('\n');
        if (akhir >= 0) pesan = pesan.substring(0, akhir);
        if (pesan.startsWith("ERROR: ")) pesan = pesan.substring(7);
        return pesan.trim();
    }
}
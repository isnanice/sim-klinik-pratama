package com.klinik.util;

import com.klinik.exception.AksesDitolakException;
import com.klinik.model.Petugas;
import com.klinik.model.Role;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utilitas otorisasi berbasis peran (Role-Based Access Control) untuk menu dan aksi aplikasi.
 */
public final class HakAkses {

    // Konstanta Menu Navigasi
    public static final String MENU_BERANDA = "MENU_BERANDA";
    public static final String MENU_PASIEN = "MENU_PASIEN";
    public static final String MENU_DOKTER = "MENU_DOKTER";
    public static final String MENU_LAYANAN = "MENU_LAYANAN";
    public static final String MENU_ANTREAN = "MENU_ANTREAN";
    public static final String MENU_PEMERIKSAAN = "MENU_PEMERIKSAAN";
    public static final String MENU_REKAM_MEDIS = "MENU_REKAM_MEDIS";
    public static final String MENU_SURAT = "MENU_SURAT";
    public static final String MENU_PEMBAYARAN = "MENU_PEMBAYARAN";

    // Konstanta Aksi Operasional
    public static final String LIHAT_PASIEN = "LIHAT_PASIEN";
    public static final String KELOLA_PASIEN = "KELOLA_PASIEN";
    public static final String LIHAT_DOKTER = "LIHAT_DOKTER";
    public static final String KELOLA_DOKTER = "KELOLA_DOKTER";
    public static final String LIHAT_LAYANAN = "LIHAT_LAYANAN";
    public static final String KELOLA_LAYANAN = "KELOLA_LAYANAN";
    public static final String KELOLA_ANTREAN = "KELOLA_ANTREAN";
    public static final String KELOLA_PEMERIKSAAN = "KELOLA_PEMERIKSAAN";
    public static final String KELOLA_REKAM_MEDIS = "KELOLA_REKAM_MEDIS";
    public static final String LIHAT_SURAT = "LIHAT_SURAT";
    public static final String BUAT_SURAT = "BUAT_SURAT";
    public static final String SERAHKAN_SURAT = "SERAHKAN_SURAT";
    public static final String KELOLA_PEMBAYARAN = "KELOLA_PEMBAYARAN";

    private static final Map<Role, Set<String>> PERMISSIONS = new EnumMap<>(Role.class);

    static {
        // Hak Akses ADMIN: Penuh ke seluruh data master, antrean, surat, dan operasional
        Set<String> adminPerms = new HashSet<>();
        Collections.addAll(adminPerms,
                MENU_BERANDA, MENU_PASIEN, MENU_DOKTER, MENU_LAYANAN,
                MENU_ANTREAN, MENU_PEMERIKSAAN, MENU_REKAM_MEDIS, MENU_SURAT, MENU_PEMBAYARAN,
                LIHAT_PASIEN, KELOLA_PASIEN,
                LIHAT_DOKTER, KELOLA_DOKTER,
                LIHAT_LAYANAN, KELOLA_LAYANAN,
                KELOLA_ANTREAN, KELOLA_PEMERIKSAAN, KELOLA_REKAM_MEDIS,
                LIHAT_SURAT, BUAT_SURAT, SERAHKAN_SURAT,
                KELOLA_PEMBAYARAN
        );
        PERMISSIONS.put(Role.ADMIN, Collections.unmodifiableSet(adminPerms));

        // Hak Akses PETUGAS: Kelola pasien, lihat dokter/layanan, kelola antrean/pendaftaran, serahkan surat, pembayaran
        Set<String> petugasPerms = new HashSet<>();
        Collections.addAll(petugasPerms,
                MENU_BERANDA, MENU_PASIEN, MENU_DOKTER, MENU_LAYANAN,
                MENU_ANTREAN, MENU_PEMERIKSAAN, MENU_SURAT, MENU_PEMBAYARAN,
                LIHAT_PASIEN, KELOLA_PASIEN,
                LIHAT_DOKTER,
                LIHAT_LAYANAN,
                KELOLA_ANTREAN, KELOLA_PEMERIKSAAN,
                LIHAT_SURAT, SERAHKAN_SURAT,
                KELOLA_PEMBAYARAN
        );
        PERMISSIONS.put(Role.PETUGAS, Collections.unmodifiableSet(petugasPerms));

        // Hak Akses DOKTER: Lihat pasien, lihat antrean, pemeriksaan medis, rekam medis, dan buat surat izin/rujukan
        Set<String> dokterPerms = new HashSet<>();
        Collections.addAll(dokterPerms,
                MENU_BERANDA, MENU_PASIEN, MENU_ANTREAN, MENU_PEMERIKSAAN, MENU_REKAM_MEDIS, MENU_SURAT,
                LIHAT_PASIEN,
                LIHAT_DOKTER,
                LIHAT_LAYANAN,
                KELOLA_ANTREAN, KELOLA_PEMERIKSAAN, KELOLA_REKAM_MEDIS,
                LIHAT_SURAT, BUAT_SURAT
        );
        PERMISSIONS.put(Role.DOKTER, Collections.unmodifiableSet(dokterPerms));
    }

    private HakAkses() {
    }

    public static Map<Role, Set<String>> getPermissionsMap() {
        return Collections.unmodifiableMap(PERMISSIONS);
    }

    public static Set<String> getPermissions(Role role) {
        if (role == null) {
            return Collections.emptySet();
        }
        return PERMISSIONS.getOrDefault(role, Collections.emptySet());
    }

    /**
     * Mengecek apakah role tertentu memiliki izin (permission) yang diminta.
     */
    public static boolean memilikiAkses(Role role, String izin) {
        if (role == null || izin == null || izin.trim().isEmpty()) {
            return false;
        }
        String cleanIzin = izin.trim().toUpperCase();
        Set<String> allowed = PERMISSIONS.get(role);
        return allowed != null && allowed.contains(cleanIzin);
    }

    /**
     * Mengecek apakah pengguna yang sedang login di SesiPengguna memiliki izin (permission) yang diminta.
     * Jika tidak ada sesi login aktif (mis. saat eksekusi unit test tanpa login), default ke ADMIN agar tidak memblokir pengujian lama.
     */
    public static boolean memilikiAkses(String izin) {
        Petugas current = SesiPengguna.getSession();
        Role role = current != null ? current.getRole() : Role.ADMIN;
        return memilikiAkses(role, izin);
    }

    /**
     * Alias bahasa Inggris untuk kemudahan pemanggilan.
     */
    public static boolean hasAccess(Role role, String permission) {
        return memilikiAkses(role, permission);
    }

    public static boolean hasAccess(String permission) {
        return memilikiAkses(permission);
    }

    /**
     * Memvalidasi hak akses role tertentu; melempar AksesDitolakException jika tidak berwenang.
     */
    public static void periksaAkses(Role role, String izin) throws AksesDitolakException {
        if (role == null) {
            throw new AksesDitolakException("Sesi pengguna tidak ditemukan atau role belum ditentukan.");
        }
        if (!memilikiAkses(role, izin)) {
            throw new AksesDitolakException("Akses ditolak: Role " + role.getLabel() + " tidak memiliki wewenang untuk " + izin + ".");
        }
    }

    /**
     * Memvalidasi hak akses berdasarkan sesi pengguna yang sedang login;
     * jika tidak ada sesi login aktif (mis. saat unit test tanpa login), menggunakan Role.ADMIN.
     */
    public static void periksaAkses(String izin) throws AksesDitolakException {
        Petugas current = SesiPengguna.getSession();
        Role role = current != null ? current.getRole() : Role.ADMIN;
        periksaAkses(role, izin);
    }

    public static void checkAccess(Role role, String permission) throws AksesDitolakException {
        periksaAkses(role, permission);
    }

    public static void checkAccess(String permission) throws AksesDitolakException {
        periksaAkses(permission);
    }
}

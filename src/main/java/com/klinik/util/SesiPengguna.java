package com.klinik.util;

import com.klinik.model.Petugas;
import com.klinik.model.Role;

/**
 * Pengelola sesi pengguna (Petugas) yang sedang login pada aplikasi desktop.
 */
public final class SesiPengguna {
    private static Petugas currentPetugas;

    private SesiPengguna() {
    }

    public static synchronized void setSession(Petugas petugas) {
        currentPetugas = petugas;
    }

    public static synchronized Petugas getSession() {
        return currentPetugas;
    }

    public static synchronized boolean isLoggedIn() {
        return currentPetugas != null;
    }

    public static synchronized void clearSession() {
        currentPetugas = null;
    }

    public static synchronized Role getCurrentRole() {
        return currentPetugas != null ? currentPetugas.getRole() : null;
    }

    public static synchronized String getCurrentUsername() {
        return currentPetugas != null ? currentPetugas.getUsername() : null;
    }

    public static synchronized String getCurrentNama() {
        return currentPetugas != null ? currentPetugas.getNama() : null;
    }
}

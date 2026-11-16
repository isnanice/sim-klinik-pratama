package com.klinik.model;

/**
 * Status kunjungan pasien (PRD BR-08). Alur satu arah dan tidak boleh melompat:
 * TERDAFTAR -> TTV_SELESAI -> DIPERIKSA -> SELESAI.
 * DIBATALKAN dapat dicapai dari tahap mana pun sebelum SELESAI (BR-15).
 *
 * Method {@link #bolehKe(StatusPemeriksaan)} adalah satu-satunya aturan transisi dan
 * hanya dipanggil dari model Pemeriksaan / PemeriksaanController.
 */
public enum StatusPemeriksaan {
    TERDAFTAR("TERDAFTAR", "Menunggu pemeriksaan awal (TTV)"),
    TTV_SELESAI("TTV_SELESAI", "Menunggu pemeriksaan dokter"),
    DIPERIKSA("DIPERIKSA", "Menunggu farmasi"),
    SELESAI("SELESAI", "Pelayanan selesai"),
    DIBATALKAN("DIBATALKAN", "Dibatalkan");

    private final String label;
    private final String keterangan;

    StatusPemeriksaan(String label, String keterangan) {
        this.label = label;
        this.keterangan = keterangan;
    }

    public String getLabel() {
        return label;
    }

    /** Penjelasan singkat tahap yang sedang ditunggu pasien. */
    public String getKeterangan() {
        return keterangan;
    }

    public boolean isFinal() {
        return this == SELESAI || this == DIBATALKAN;
    }

    /** Status berikutnya pada alur normal; null bila sudah SELESAI atau DIBATALKAN. */
    public StatusPemeriksaan berikutnya() {
        switch (this) {
            case TERDAFTAR:   return TTV_SELESAI;
            case TTV_SELESAI: return DIPERIKSA;
            case DIPERIKSA:   return SELESAI;
            default:          return null;
        }
    }

    /** Aturan transisi tunggal: hanya ke tahap berikutnya, atau DIBATALKAN dari status non-final. */
    public boolean bolehKe(StatusPemeriksaan tujuan) {
        if (tujuan == null || isFinal()) {
            return false;
        }
        return tujuan == DIBATALKAN || tujuan == berikutnya();
    }

    @Override
    public String toString() {
        return label;
    }

    public static StatusPemeriksaan fromString(String text) {
        if (text == null) return null;
        String t = text.trim();
        for (StatusPemeriksaan s : values()) {
            if (s.name().equalsIgnoreCase(t) || s.label.equalsIgnoreCase(t)) {
                return s;
            }
        }
        return null;
    }
}

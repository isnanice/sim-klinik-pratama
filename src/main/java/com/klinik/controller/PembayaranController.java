package com.klinik.controller;

import com.klinik.dao.PembayaranDAO;
import com.klinik.dao.PemeriksaanDAO;
import com.klinik.exception.AksesDitolakException;
import com.klinik.exception.DataNotFoundException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.model.Layanan;
import com.klinik.model.MetodePembayaran;
import com.klinik.model.Pembayaran;
import com.klinik.model.Pemeriksaan;
import com.klinik.model.StatusPembayaran;
import com.klinik.model.StatusPemeriksaan;
import com.klinik.util.HakAkses;
import com.klinik.validation.Validator;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller pembayaran (FR-25 sampai FR-28). Total dihitung lewat polimorfisme
 * {@link Layanan#hitungTarif(double)}: referensi bertipe Layanan, objek LayananUmum atau LayananSpesialis.
 *
 * Keputusan terbuka PRD no. 6: belum ada aturan khusus pasien BPJS, sehingga semua kunjungan SELESAI
 * dihitung dengan tarif layanan. Bila diputuskan BPJS ditanggung, ubah {@link #hitungTotalBiaya}.
 */
public class PembayaranController {
    private final PembayaranDAO pembayaranDAO;
    private final PemeriksaanDAO pemeriksaanDAO;

    public PembayaranController(PembayaranDAO pembayaranDAO, PemeriksaanDAO pemeriksaanDAO) {
        this.pembayaranDAO = pembayaranDAO;
        this.pemeriksaanDAO = pemeriksaanDAO;
    }

    /** Riwayat pembayaran (FR-28), terbaru di atas. */
    public List<Pembayaran> getAllPembayaran() throws AksesDitolakException {
        HakAkses.periksaAkses(HakAkses.KELOLA_PEMBAYARAN);
        return pembayaranDAO.findAll();
    }

    public Optional<Pembayaran> getPembayaranByPemeriksaanId(String idPemeriksaan) throws AksesDitolakException {
        HakAkses.periksaAkses(HakAkses.KELOLA_PEMBAYARAN);
        return pembayaranDAO.findByPemeriksaanId(idPemeriksaan);
    }

    /** Kunjungan SELESAI yang belum punya pembayaran, untuk pilihan di PembayaranView (BR-10). */
    public List<Pemeriksaan> kunjunganSiapBayar() throws AksesDitolakException {
        HakAkses.periksaAkses(HakAkses.KELOLA_PEMBAYARAN);
        Set<String> sudahBayar = new HashSet<>();
        for (Pembayaran b : pembayaranDAO.findAll()) {
            if (b.getPemeriksaan() != null) sudahBayar.add(b.getPemeriksaan().getId());
        }
        return pemeriksaanDAO.findByStatus(StatusPemeriksaan.SELESAI).stream()
                .filter(p -> !sudahBayar.contains(p.getId()))
                .collect(Collectors.toList());
    }

    /** Menghitung total lewat method polimorfik hitungTarif (overriding dan overloading dipakai). */
    public double hitungTotalBiaya(Layanan layanan, double biayaTambahan) throws DataTidakValidException {
        if (layanan == null) {
            throw new DataTidakValidException("Layanan", "Layanan tidak boleh kosong saat menghitung tarif.");
        }
        Validator.validateNonNegativeNumber(biayaTambahan, "Biaya Tambahan");
        return layanan.hitungTarif(biayaTambahan);
    }

    /** Menyimpan pembayaran dalam satu transaksi (BR-12) untuk kunjungan berstatus SELESAI (BR-10). */
    public Pembayaran prosesPembayaran(String idPemeriksaan, double biayaTambahan,
                                       MetodePembayaran metode, StatusPembayaran status)
            throws AksesDitolakException, DataNotFoundException, DataTidakValidException {
        HakAkses.periksaAkses(HakAkses.KELOLA_PEMBAYARAN);
        if (idPemeriksaan == null || idPemeriksaan.trim().isEmpty()) {
            throw new DataTidakValidException("Pemeriksaan", "Kunjungan yang dibayar wajib dipilih.");
        }
        Pemeriksaan p = pemeriksaanDAO.findById(idPemeriksaan.trim())
                .orElseThrow(() -> new DataNotFoundException("Pemeriksaan dengan ID " + idPemeriksaan + " tidak ditemukan."));
        if (p.getStatus() != StatusPemeriksaan.SELESAI) {
            throw new DataTidakValidException("Status Pemeriksaan",
                    "Pembayaran hanya dapat diproses untuk pemeriksaan yang berstatus SELESAI.");
        }
        if (pembayaranDAO.findByPemeriksaanId(p.getId()).isPresent()) {
            throw new DataTidakValidException("Pembayaran", "Pemeriksaan ini sudah memiliki data pembayaran.");
        }
        double total = hitungTotalBiaya(p.getLayanan(), biayaTambahan);
        Pembayaran b = new Pembayaran(null, p, LocalDate.now(), biayaTambahan, total,
                metode != null ? metode : MetodePembayaran.TUNAI,
                status != null ? status : StatusPembayaran.LUNAS);
        pembayaranDAO.insert(b);              // transaksi dan ID dari database
        return b;
    }

    /** Mengubah status (misalnya BELUM_LUNAS menjadi LUNAS). Yang sudah LUNAS tidak dapat diubah (BR-10). */
    public void updateStatusPembayaran(String idPembayaran, StatusPembayaran statusBaru)
            throws AksesDitolakException, DataNotFoundException, DataTidakValidException {
        HakAkses.periksaAkses(HakAkses.KELOLA_PEMBAYARAN);
        Pembayaran b = pembayaranDAO.findById(idPembayaran)
                .orElseThrow(() -> new DataNotFoundException("Pembayaran dengan ID " + idPembayaran + " tidak ditemukan."));
        b.ubahStatus(statusBaru);
        pembayaranDAO.update(b);
    }
}

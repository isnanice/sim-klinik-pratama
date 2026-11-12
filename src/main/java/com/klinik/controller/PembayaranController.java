package com.klinik.controller;

import com.klinik.dao.PembayaranDAO;
import com.klinik.dao.PemeriksaanDAO;
import com.klinik.exception.DataNotFoundException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.model.*;
import com.klinik.validation.Validator;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Controller untuk mengelola transaksi pembayaran layanan klinik.
 * Menerapkan polimorfisme Layanan.hitungTarif(biayaTambahan).
 */
public class PembayaranController {
    private final PembayaranDAO pembayaranDAO;
    private final PemeriksaanDAO pemeriksaanDAO;

    public PembayaranController(PembayaranDAO pembayaranDAO, PemeriksaanDAO pemeriksaanDAO) {
        this.pembayaranDAO = pembayaranDAO;
        this.pemeriksaanDAO = pemeriksaanDAO;
    }

    public List<Pembayaran> getAllPembayaran() {
        return pembayaranDAO.findAll();
    }

    public Optional<Pembayaran> getPembayaranByPemeriksaanId(String idPemeriksaan) {
        return pembayaranDAO.findByPemeriksaanId(idPemeriksaan);
    }

    /**
     * Menghitung total biaya dengan memanggil method polimorfik hitungTarif pada kelas Layanan.
     */
    public double hitungTotalBiaya(Layanan layanan, double biayaTambahan) throws DataTidakValidException {
        if (layanan == null) {
            throw new DataTidakValidException("Layanan", "Layanan tidak boleh kosong saat menghitung tarif.");
        }
        Validator.validateNonNegativeNumber(biayaTambahan, "Biaya Tambahan");
        return layanan.hitungTarif(biayaTambahan);
    }

    /**
     * Membuat transaksi pembayaran baru untuk pemeriksaan yang telah SELESAI (BR-10).
     */
    public Pembayaran prosesPembayaran(String idPemeriksaan, double biayaTambahan,
                                       MetodePembayaran metode, StatusPembayaran status)
            throws DataNotFoundException, DataTidakValidException {

        Pemeriksaan p = pemeriksaanDAO.findById(idPemeriksaan)
                .orElseThrow(() -> new DataNotFoundException("Pemeriksaan dengan ID " + idPemeriksaan + " tidak ditemukan."));

        if (p.getStatus() != StatusPemeriksaan.SELESAI) {
            throw new DataTidakValidException("Status Pemeriksaan",
                    "Pembayaran hanya dapat diproses untuk pemeriksaan yang berstatus SELESAI.");
        }

        if (pembayaranDAO.findByPemeriksaanId(idPemeriksaan).isPresent()) {
            throw new DataTidakValidException("Pembayaran", "Pemeriksaan ini sudah memiliki data pembayaran.");
        }

        Validator.validateNonNegativeNumber(biayaTambahan, "Biaya Tambahan");

        double total = hitungTotalBiaya(p.getLayanan(), biayaTambahan);
        Pembayaran byr = new Pembayaran(
                pembayaranDAO.generateNextId(),
                p,
                LocalDate.now(),
                biayaTambahan,
                total,
                metode != null ? metode : MetodePembayaran.TUNAI,
                status != null ? status : StatusPembayaran.LUNAS
        );

        pembayaranDAO.insert(byr);
        return byr;
    }

    public void updateStatusPembayaran(String idPembayaran, StatusPembayaran statusBaru)
            throws DataNotFoundException, DataTidakValidException {
        Pembayaran b = pembayaranDAO.findById(idPembayaran)
                .orElseThrow(() -> new DataNotFoundException("Pembayaran dengan ID " + idPembayaran + " tidak ditemukan."));

        if (b.getStatusPembayaran() == StatusPembayaran.LUNAS) {
            throw new DataTidakValidException("Status Pembayaran", "Pembayaran yang sudah LUNAS tidak dapat diubah lagi.");
        }

        b.setStatusPembayaran(statusBaru);
        pembayaranDAO.update(b);
    }
}


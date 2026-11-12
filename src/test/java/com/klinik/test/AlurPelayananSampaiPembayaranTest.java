package com.klinik.test;

import com.klinik.controller.*;
import com.klinik.dao.*;
import com.klinik.exception.*;
import com.klinik.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pengujian komprehensif alur pelayanan klinik dari pendaftaran hingga pembayaran,
 * memverifikasi 4 pilar PBO (Enkapsulasi, Pewarisan, Polimorfisme, Abstraksi),
 * aturan bisnis (BR), dan exception handling.
 */
public class AlurPelayananSampaiPembayaranTest {

    private PasienDAO pasienDAO;
    private DokterDAO dokterDAO;
    private LayananDAO layananDAO;
    private PemeriksaanDAO pemeriksaanDAO;
    private RekamMedisDAO rekamMedisDAO;
    private PembayaranDAO pembayaranDAO;

    private DokterController dokterController;
    private LayananController layananController;
    private RekamMedisController rekamMedisController;
    private PembayaranController pembayaranController;

    @BeforeEach
    void setUp() {
        pasienDAO = new PasienDAO();
        dokterDAO = new DokterDAO();
        layananDAO = new LayananDAO();
        pemeriksaanDAO = new PemeriksaanDAO(pasienDAO, dokterDAO, layananDAO);
        rekamMedisDAO = new RekamMedisDAO(pemeriksaanDAO, pasienDAO, dokterDAO);
        pembayaranDAO = new PembayaranDAO(pemeriksaanDAO);

        dokterController = new DokterController(dokterDAO, pemeriksaanDAO, Role.ADMIN);
        layananController = new LayananController(layananDAO, Role.ADMIN);
        rekamMedisController = new RekamMedisController(rekamMedisDAO, pemeriksaanDAO);
        pembayaranController = new PembayaranController(pembayaranDAO, pemeriksaanDAO);
    }

    @Test
    @DisplayName("1. Uji Polimorfisme: Perhitungan tarif LayananUmum vs LayananSpesialis")
    void testPolimorfismeTarifLayanan() {
        // LayananUmum: tarif = dasar + tambahan
        Layanan layananUmum = new LayananUmum("LYN-099", "Konsultasi Umum", 75000.0, "Konsultasi umum", true);
        double tarifUmum = layananUmum.hitungTarif(10000.0);
        assertEquals(85000.0, tarifUmum, "Layanan umum harus 75.000 + 10.000 = 85.000");

        // LayananSpesialis: tarif = dasar + tambahan + biaya spesialis (50.000)
        Layanan layananSpesialis = new LayananSpesialis("LYN-100", "Konsultasi Penyakit Dalam", 150000.0, "Spesialis", true, 50000.0);
        double tarifSpesialis = layananSpesialis.hitungTarif(25000.0);
        assertEquals(225000.0, tarifSpesialis, "Layanan spesialis harus 150.000 + 25.000 + 50.000 = 225.000 (sesuai C8/Mockup)");

        // Pemanggilan lewat antarmuka LayananMedis (Abstraksi & Polimorfisme)
        LayananMedis medis = layananSpesialis;
        assertEquals(225000.0, medis.hitungTarif(25000.0));
    }

    @Test
    @DisplayName("2. Uji Alur Lengkap: Pelayanan Pasien dari Pendaftaran, Rekam Medis, hingga Pembayaran")
    void testAlurPelayananSampaiPembayaranLengkap() throws Exception {
        // Step 1: Pasien mendaftar
        Pasien pasien = new Pasien("PSN-TEST-1", "Budi Santoso", JenisKelamin.LAKI_LAKI,
                LocalDate.of(1990, 5, 12), "Jl. Kaliurang", "081234567890", "0001234567890", LocalDate.now(), true);
        pasienDAO.insert(pasien);

        // Step 2: Dokter spesialis tersedia
        Dokter dokter = new Dokter("DKT-TEST-1", "dr. Hadi Nugroho, Sp.PD", JenisKelamin.LAKI_LAKI,
                "Yogyakarta", "081298765432", "STR-12345678", "Penyakit Dalam", true);
        dokterDAO.insert(dokter);

        // Step 3: Layanan Spesialis dipilih
        Layanan layanan = new LayananSpesialis("LYN-TEST-1", "Konsultasi Penyakit Dalam", 150000.0, "Spesialis", true, 50000.0);
        layananDAO.insert(layanan);

        // Step 4: Kunjungan pemeriksaan dibuat (Status: TERDAFTAR -> MENUNGGU_DOKTER)
        Pemeriksaan pemeriksaan = new Pemeriksaan("PMR-TEST-1", "P-007", pasien, dokter, layanan,
                LocalDate.now(), "Demam dan nyeri ulu hati", "petugas01", StatusPemeriksaan.MENUNGGU_DOKTER);
        pemeriksaanDAO.insert(pemeriksaan);

        // Step 5: Dokter melakukan pemeriksaan dan mencatat Rekam Medis (RekamMedisController)
        RekamMedis rm = new RekamMedis();
        rm.setPemeriksaan(pemeriksaan);
        rm.setPasien(pasien);
        rm.setDokter(dokter);
        rm.setDiagnosis("Gastritis Akut dan Dispepsia");
        rm.setTindakan("Pemberian antasida oral dan edukasi diet lambung");
        rm.setResepObat("Antasida sirup 3x1 cth, Omeprazole 20mg 2x1");
        rm.setCatatan("Hindari makanan pedas dan asam selama 1 minggu.");

        RekamMedis tersimpan = rekamMedisController.simpanAtauUbahRekamMedis(rm);
        assertNotNull(tersimpan.getId(), "Rekam Medis harus memiliki ID yang ter-generate");

        // Pemeriksaan beralih ke MENUNGGU_OBAT setelah dokter mencatat rekam medis
        assertEquals(StatusPemeriksaan.MENUNGGU_OBAT, pemeriksaan.getStatus());

        // Step 6: Pasien menerima obat di farmasi, pemeriksaan diselesaikan (Status: SELESAI)
        rekamMedisController.selesaikanPemeriksaan(pemeriksaan.getId());
        assertEquals(StatusPemeriksaan.SELESAI, pemeriksaanDAO.findById(pemeriksaan.getId()).get().getStatus());

        // Step 7: Kasir memproses Pembayaran (PembayaranController)
        // Biaya tambahan obat/tindakan: 25.000
        double biayaTambahan = 25000.0;
        Pembayaran pembayaran = pembayaranController.prosesPembayaran(
                pemeriksaan.getId(),
                biayaTambahan,
                MetodePembayaran.TRANSFER,
                StatusPembayaran.LUNAS
        );

        // Verifikasi hasil perhitungan polimorfik: 150.000 + 25.000 + 50.000 = 225.000
        assertEquals(225000.0, pembayaran.getTotalBiaya(), 0.001);
        assertEquals(StatusPembayaran.LUNAS, pembayaran.getStatusPembayaran());
        assertEquals(MetodePembayaran.TRANSFER, pembayaran.getMetodePembayaran());
        assertNotNull(pembayaran.getId());

        // Step 8: Verifikasi ringkasan cetak (DapatDicetak)
        String ringkasan = pembayaran.cetakRingkasan();
        assertTrue(ringkasan.contains("Total Biaya"));
        assertTrue(ringkasan.contains("Rp 225.000"));
    }

    @Test
    @DisplayName("3. Uji Validasi Bisnis BR-04: Format No. Telepon Dokter")
    void testValidasiNomorTeleponDokter() {
        Dokter d = new Dokter();
        d.setNama("dr. Test Validasi");
        d.setJenisKelamin(JenisKelamin.LAKI_LAKI);
        d.setNomorRegistrasi("STR-999");
        d.setBidangPelayanan("Umum");

        // Format salah (tidak diawali 08 / +628)
        d.setNoTelepon("02112345678");
        assertThrows(DataTidakValidException.class, () -> dokterController.tambahDokter(d));

        // Format benar
        d.setNoTelepon("081234567899");
        assertDoesNotThrow(() -> dokterController.tambahDokter(d));
    }

    @Test
    @DisplayName("4. Uji Validasi Bisnis BR-06: Tarif Layanan Tidak Boleh Negatif")
    void testValidasiTarifLayananTidakNegatif() {
        Layanan l = new LayananUmum();
        l.setNamaLayanan("Layanan Uji Negatif");
        l.setTarifDasar(-50000.0); // Negatif

        assertThrows(DataTidakValidException.class, () -> layananController.tambahLayanan(l));
    }

    @Test
    @DisplayName("5. Uji Validasi Bisnis BR-11: Dokter dengan riwayat pemeriksaan tidak dapat dihapus permanen")
    void testHapusDokterDenganRiwayatPemeriksaanDitolak() {
        // Ambil dokter yang memiliki riwayat pemeriksaan
        List<Dokter> list = dokterDAO.findAll();
        assertFalse(list.isEmpty());
        Dokter dokterTerpakai = list.get(0);

        // Buat pemeriksaan yang mengacu ke dokter ini
        Pasien p = pasienDAO.findAll().get(0);
        Layanan l = layananDAO.findAll().get(0);
        Pemeriksaan pmr = new Pemeriksaan(pemeriksaanDAO.generateNextId(), "P-099", p, dokterTerpakai, l,
                LocalDate.now(), "Uji integritas", "petugas01", StatusPemeriksaan.TERDAFTAR);
        pemeriksaanDAO.insert(pmr);

        // Hapus harus melempar DataMasihDipakaiException
        assertThrows(DataMasihDipakaiException.class, () -> dokterController.hapusDokter(dokterTerpakai.getId()));

        // Namun bisa dinonaktifkan
        assertDoesNotThrow(() -> dokterController.nonaktifkanDokter(dokterTerpakai.getId(), false));
        assertFalse(dokterDAO.findById(dokterTerpakai.getId()).get().isAktif());
    }

    @Test
    @DisplayName("6. Uji Kontrol Akses Berbasis Peran: PETUGAS tidak berhak menambah/mengubah Dokter")
    void testAksesDitolakRolePetugas() {
        // Ubah role controller ke PETUGAS
        dokterController.setCurrentRole(Role.PETUGAS);

        Dokter d = new Dokter("DKT-NEW", "dr. Baru", JenisKelamin.PEREMPUAN, "Alamat",
                "081234567890", "STR-111", "Umum", true);

        assertThrows(AksesDitolakException.class, () -> dokterController.tambahDokter(d));
        assertThrows(AksesDitolakException.class, () -> dokterController.hapusDokter("DKT-001"));
    }
}


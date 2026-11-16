package com.klinik.test;

import com.klinik.exception.DataTidakValidException;
import com.klinik.model.Dokter;
import com.klinik.model.JenisKelamin;
import com.klinik.model.JenisPenjamin;
import com.klinik.model.Layanan;
import com.klinik.model.LayananSpesialis;
import com.klinik.model.LayananUmum;
import com.klinik.model.MetodePembayaran;
import com.klinik.model.Pasien;
import com.klinik.model.Pembayaran;
import com.klinik.model.Pemeriksaan;
import com.klinik.model.RekamMedis;
import com.klinik.model.StatusPembayaran;
import com.klinik.model.StatusPemeriksaan;

import java.time.LocalDate;

/**
 * Pengujian aturan bisnis pada lapisan model (tanpa database, tanpa JUnit): alur status BR-08,
 * pembatalan BR-14/15, rekam medis bertahap BR-09, BPJS BR-16, polimorfisme tarif, kunci LUNAS BR-10.
 * Jalankan: java -cp target/classes;target/test-classes com.klinik.test.AturanModelTest
 * Keluar dengan kode 1 bila ada pengujian yang gagal.
 */
public class AturanModelTest {
    private static int lulus = 0;
    private static int gagal = 0;

    private interface Aksi { void jalan() throws Exception; }

    private static void uji(String nama, Aksi aksi) {
        try {
            aksi.jalan();
            lulus++;
            System.out.println("  [LULUS] " + nama);
        } catch (Throwable t) {
            gagal++;
            System.out.println("  [GAGAL] " + nama + " -> " + t);
        }
    }

    private static void harusDitolak(Aksi aksi) throws Exception {
        try {
            aksi.jalan();
        } catch (DataTidakValidException e) {
            return;
        }
        throw new AssertionError("seharusnya ditolak dengan DataTidakValidException");
    }

    private static void harus(boolean kondisi, String pesan) {
        if (!kondisi) throw new AssertionError(pesan);
    }

    private static Pasien pasien(String bpjs) {
        return new Pasien("PSN-001", "3201010101010001", "Budi Santoso", JenisKelamin.LAKI_LAKI,
                LocalDate.of(1990, 1, 1), "Jl. Mawar 1", "081234567890", bpjs);
    }

    private static Dokter dokter() {
        return new Dokter("DKT-001", "dr. Ani", "Jl. Melati 2", "081200000000", "STR-1", "Umum");
    }

    private static Pemeriksaan kunjungan(Layanan l, StatusPemeriksaan s) {
        return new Pemeriksaan("PMR-001", pasien(""), dokter(), l, "Demam", "3 hari", JenisPenjamin.NON_BPJS, s);
    }

    private static Layanan umum() {
        return new LayananUmum("LYN-001", "Konsultasi Umum", 50000, "", true);
    }

    private static Layanan spesialis() {
        return new LayananSpesialis("LYN-002", "Konsultasi Spesialis", 100000, "", true);
    }

    public static void main(String[] args) {
        System.out.println("== Alur status (BR-08) ==");
        uji("status maju berurutan sampai SELESAI", () -> {
            Pemeriksaan p = kunjungan(umum(), StatusPemeriksaan.TERDAFTAR);
            p.majuKe(StatusPemeriksaan.TTV_SELESAI);
            p.majuKe(StatusPemeriksaan.DIPERIKSA);
            p.majuKe(StatusPemeriksaan.SELESAI);
            harus(p.getStatus() == StatusPemeriksaan.SELESAI, "status akhir harus SELESAI");
        });
        uji("lompat tahap ditolak", () -> harusDitolak(
                () -> kunjungan(umum(), StatusPemeriksaan.TERDAFTAR).majuKe(StatusPemeriksaan.DIPERIKSA)));
        uji("mundur tahap ditolak", () -> harusDitolak(
                () -> kunjungan(umum(), StatusPemeriksaan.DIPERIKSA).majuKe(StatusPemeriksaan.TERDAFTAR)));
        uji("status SELESAI tidak dapat diubah lagi", () -> harusDitolak(
                () -> kunjungan(umum(), StatusPemeriksaan.SELESAI).majuKe(StatusPemeriksaan.DIBATALKAN)));

        System.out.println("== Panggilan dan pembatalan (BR-14, BR-15) ==");
        uji("batal sebelum 3 kali dipanggil ditolak", () -> {
            Pemeriksaan p = kunjungan(umum(), StatusPemeriksaan.TERDAFTAR);
            p.panggil();
            p.panggil();
            harus(!p.sudahBolehDibatalkan(), "baru 2 panggilan, belum boleh batal");
            harusDitolak(() -> p.batalkan("tidak hadir", "petugas"));
        });
        uji("batal setelah 3 kali dipanggil berhasil", () -> {
            Pemeriksaan p = kunjungan(umum(), StatusPemeriksaan.TERDAFTAR);
            p.panggil();
            p.panggil();
            p.panggil();
            p.batalkan("tidak hadir", "petugas");
            harus(p.getStatus() == StatusPemeriksaan.DIBATALKAN, "status harus DIBATALKAN");
            harus("tidak hadir".equals(p.getAlasanBatal()), "alasan tersimpan");
        });
        uji("alasan pembatalan wajib", () -> {
            Pemeriksaan p = kunjungan(umum(), StatusPemeriksaan.TERDAFTAR);
            p.panggil();
            p.panggil();
            p.panggil();
            harusDitolak(() -> p.batalkan("  ", "petugas"));
        });
        uji("panggilan kembali ke 0 saat status berubah", () -> {
            Pemeriksaan p = kunjungan(umum(), StatusPemeriksaan.TERDAFTAR);
            p.panggil();
            p.majuKe(StatusPemeriksaan.TTV_SELESAI);
            harus(p.getJumlahPanggilan() == 0, "jumlah panggilan harus di-reset");
        });

        System.out.println("== Pendaftaran (BR-07, BR-16) ==");
        uji("BPJS tanpa nomor BPJS ditolak", () -> {
            Pemeriksaan p = new Pemeriksaan("PMR-002", pasien(""), dokter(), umum(), "Batuk", "2 hari",
                    JenisPenjamin.BPJS, StatusPemeriksaan.TERDAFTAR);
            harusDitolak(p::validasiPendaftaran);
        });
        uji("BPJS dengan nomor BPJS diterima", () -> {
            Pemeriksaan p = new Pemeriksaan("PMR-003", pasien("0001234567890"), dokter(), umum(), "Batuk", "2 hari",
                    JenisPenjamin.BPJS, StatusPemeriksaan.TERDAFTAR);
            p.validasiPendaftaran();
        });
        uji("keluhan kosong ditolak", () -> {
            Pemeriksaan p = new Pemeriksaan("PMR-004", pasien(""), dokter(), umum(), " ", "2 hari",
                    JenisPenjamin.NON_BPJS, StatusPemeriksaan.TERDAFTAR);
            harusDitolak(p::validasiPendaftaran);
        });

        System.out.println("== Rekam medis bertahap (BR-09) ==");
        uji("TTV tidak boleh diisi bila status bukan TERDAFTAR", () -> harusDitolak(
                () -> new RekamMedis(kunjungan(umum(), StatusPemeriksaan.DIPERIKSA)).isiTtv(120, 80, 170, 60)));
        uji("pemeriksaan dokter sebelum TTV ditolak", () -> harusDitolak(
                () -> new RekamMedis(kunjungan(umum(), StatusPemeriksaan.TERDAFTAR))
                        .isiPemeriksaanDokter("3 hari", "Suhu 38", "Influenza", "Paracetamol", "")));
        uji("TTV tidak masuk akal ditolak", () -> {
            RekamMedis rm = new RekamMedis(kunjungan(umum(), StatusPemeriksaan.TERDAFTAR));
            harusDitolak(() -> rm.isiTtv(80, 120, 170, 60));      // sistolik harus lebih besar dari diastolik
            harusDitolak(() -> rm.isiTtv(120, 80, 170, -5));
        });
        uji("tiga tahap terisi berurutan", () -> {
            Pemeriksaan p = kunjungan(umum(), StatusPemeriksaan.TERDAFTAR);
            RekamMedis rm = new RekamMedis(p);
            rm.isiTtv(120, 80, 170, 65);
            harus(rm.punyaTtv(), "TTV terisi");
            p.majuKe(StatusPemeriksaan.TTV_SELESAI);
            rm.isiPemeriksaanDokter("3 hari", "Suhu 38", "Influenza", "Paracetamol 3x1", "Banyak minum");
            harus(rm.punyaPemeriksaanDokter(), "pemeriksaan dokter terisi");
            p.majuKe(StatusPemeriksaan.DIPERIKSA);
            rm.isiFarmasi("Paracetamol 10 tablet");
            harus(rm.punyaFarmasi(), "farmasi terisi");
        });
        uji("diagnosis wajib diisi dokter", () -> {
            RekamMedis rm = new RekamMedis(kunjungan(umum(), StatusPemeriksaan.TTV_SELESAI));
            harusDitolak(() -> rm.isiPemeriksaanDokter("3 hari", "Suhu 38", " ", "Paracetamol", ""));
        });

        System.out.println("== Tarif polimorfik ==");
        uji("Umum dan Spesialis menghitung tarif berbeda lewat referensi Layanan", () -> {
            Layanan a = umum();
            Layanan b = spesialis();
            harus(a.hitungTarif() == 50000, "tarif umum = tarif dasar");
            harus(b.hitungTarif() > b.getTarifDasar(), "tarif spesialis memakai biaya konsultasi tambahan");
            harus(b.hitungTarif() > a.hitungTarif(), "spesialis lebih mahal");
        });
        uji("overload hitungTarif(biayaTambahan) menjumlahkan biaya tambahan", () -> {
            Layanan a = umum();
            harus(a.hitungTarif(15000) == a.hitungTarif() + 15000, "tarif + biaya tambahan");
        });
        uji("rincianTarif spesialis menyebut biaya spesialis", () ->
                harus(spesialis().rincianTarif().contains("spesialis"), "rincian spesialis"));

        System.out.println("== Pembayaran (BR-10) ==");
        uji("biaya tambahan negatif ditolak", () -> {
            Pembayaran b = new Pembayaran();
            harusDitolak(() -> b.setBiayaTambahan(-1));
        });
        uji("BELUM_LUNAS dapat menjadi LUNAS", () -> {
            Pembayaran b = new Pembayaran("BYR-001", kunjungan(umum(), StatusPemeriksaan.SELESAI), LocalDate.now(),
                    0, 50000, MetodePembayaran.TUNAI, StatusPembayaran.BELUM_LUNAS);
            b.ubahStatus(StatusPembayaran.LUNAS);
            harus(b.getStatusPembayaran() == StatusPembayaran.LUNAS, "harus LUNAS");
        });
        uji("pembayaran LUNAS terkunci", () -> {
            Pembayaran b = new Pembayaran("BYR-002", kunjungan(umum(), StatusPemeriksaan.SELESAI), LocalDate.now(),
                    0, 50000, MetodePembayaran.TRANSFER, StatusPembayaran.LUNAS);
            harusDitolak(() -> b.ubahStatus(StatusPembayaran.BELUM_LUNAS));
        });

        System.out.println();
        System.out.println("Hasil: " + lulus + " lulus, " + gagal + " gagal.");
        if (gagal > 0) System.exit(1);
    }
}

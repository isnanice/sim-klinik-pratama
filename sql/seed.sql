-- seed.sql : data awal untuk pengembangan dan demo
-- Jalankan SETELAH schema.sql: psql -U klinik_user -d db_klinik_pratama -v ON_ERROR_STOP=1 -f sql/seed.sql
-- Akun login (hanya untuk lingkungan lokal, ganti bila dipakai di tempat lain):
--     admin   / admin123     (role ADMIN)
--     petugas / petugas123   (role PETUGAS)
--     dokter  / dokter123    (role DOKTER, dr. Ani Wijaya)
-- Tanggal kunjungan memakai CURRENT_DATE supaya antrean "hari ini" selalu terisi.

BEGIN;

-- ---------------------------------------------------------------------
-- Dokter
-- ---------------------------------------------------------------------
INSERT INTO dokter (id_dokter, nama, jenis_kelamin, alamat, no_telepon, nomor_registrasi, bidang_pelayanan, status_aktif) VALUES
('DKT-001', 'dr. Andi Pratama, Sp.PD', 'Laki-laki', 'Jl. Melati No. 12, Bandung',     '081122334455', 'SIP-3204-2021-089', 'Penyakit Dalam', TRUE),
('DKT-002', 'dr. Ratna Sari, Sp.A',    'Perempuan', 'Jl. Kenanga No. 7, Bandung',     '081233445566', 'SIP-3204-2020-045', 'Anak',           TRUE),
('DKT-003', 'dr. Hendra Gunawan',      'Laki-laki', 'Jl. Mawar No. 21, Bandung',      '081344556677', 'SIP-3204-2022-112', 'Umum',           TRUE),
('DKT-004', 'drg. Maya Putri',         'Perempuan', 'Jl. Anggrek No. 3, Bandung',     '081555667788', 'SIP-3204-2019-033', 'Gigi',           TRUE),
('DKT-005', 'dr. Surya Wijaya, Sp.JP', 'Laki-laki', 'Jl. Dahlia No. 18, Bandung',     '081666778899', 'SIP-3204-2023-156', 'Jantung',        TRUE),
('DKT-006', 'dr. Ani Wijaya',          'Perempuan', 'Jl. Cempaka No. 9, Bandung',     '081777889900', 'SIP-3204-2024-201', 'Umum',           TRUE);

-- ---------------------------------------------------------------------
-- Akun login. password_hash = salt(hex) ':' SHA-256(salt + password)(hex), persis format PasswordUtil.hashPassword().
-- ---------------------------------------------------------------------
INSERT INTO petugas (id_petugas, nama, alamat, no_telepon, username, password_hash, role, id_dokter, status_aktif) VALUES
('PTG-001', 'Siti Rahma',    'Jl. Asia Afrika No. 1, Bandung', '081100000001', 'admin',
 '84675585cb37df85016465de63822335:8f3bdb390a7c25602b4dc0b27141cb1c897b3802702da92106756788ac377900', 'ADMIN',   NULL,      TRUE),
('PTG-002', 'Rudi Hartono',  'Jl. Braga No. 2, Bandung',       '081100000002', 'petugas',
 'a4367203e444ad6212d5446d5f0a77b0:b9e173e77601f1ca5f110ba813bdb3e4c957fa3718f2c38b963cd9908b8a7d98', 'PETUGAS', NULL,      TRUE),
('PTG-003', 'dr. Ani Wijaya', 'Jl. Cempaka No. 9, Bandung',    '081777889900', 'dokter',
 '93845b4d04a8f65f52cdb831e898db12:2a57640d4cabcc98961bbe652e1fd444b0058e489956bef04cfe7fdfc15a7ec0', 'DOKTER',  'DKT-006', TRUE);

-- ---------------------------------------------------------------------
-- Layanan (tarif dasar; layanan SPESIALIS ditambah biaya konsultasi di kode Java)
-- ---------------------------------------------------------------------
INSERT INTO layanan (id_layanan, nama_layanan, jenis_layanan, tarif, keterangan, status_aktif) VALUES
('LYN-001', 'Konsultasi Dokter Umum',        'UMUM',      75000,  'Pemeriksaan kesehatan dasar, anamnesa, dan konsultasi keluhan umum.', TRUE),
('LYN-002', 'Pemeriksaan Kesehatan Gigi',    'UMUM',      90000,  'Pemeriksaan gigi dan mulut, pembersihan karang gigi ringan.',          TRUE),
('LYN-003', 'Konsultasi Penyakit Dalam',     'SPESIALIS', 150000, 'Konsultasi dengan dokter spesialis penyakit dalam.',                   TRUE),
('LYN-004', 'Konsultasi Dokter Anak',        'SPESIALIS', 140000, 'Konsultasi tumbuh kembang dan penyakit anak.',                         TRUE),
('LYN-005', 'Tes Laboratorium Darah Rutin',  'UMUM',      110000, 'Pemeriksaan darah rutin.',                                             FALSE);

-- ---------------------------------------------------------------------
-- Pasien (NIK dan nomor BPJS adalah data contoh, bukan data nyata)
-- ---------------------------------------------------------------------
INSERT INTO pasien (id_pasien, nik, no_bpjs, nama, jenis_kelamin, tanggal_lahir, alamat, no_telepon, tanggal_registrasi, status_aktif) VALUES
('PSN-001', '3273010000000001', '0001234567890', 'Budi Santoso',   'Laki-laki', DATE '1990-05-12', 'Jl. Merdeka No. 45, Bandung',    '081234567890', CURRENT_DATE - 30, TRUE),
('PSN-002', '3273010000000002', NULL,            'Dewi Sartika',   'Perempuan', DATE '1985-08-24', 'Jl. Dago Atas No. 12, Bandung',  '085712349988', CURRENT_DATE - 30, TRUE),
('PSN-003', '3273010000000003', '0001234567891', 'Ahmad Fauzi',    'Laki-laki', DATE '1995-01-03', 'Jl. Riau No. 88, Bandung',       '087890123456', CURRENT_DATE - 25, TRUE),
('PSN-004', '3273010000000004', NULL,            'Ratna Sari',     'Perempuan', DATE '1992-11-17', 'Jl. Cihampelas No. 102, Bandung','081322445566', CURRENT_DATE - 25, TRUE),
('PSN-005', '3273010000000005', NULL,            'Eko Prasetyo',   'Laki-laki', DATE '1978-04-09', 'Jl. Supratman No. 23, Bandung',  '082199887711', CURRENT_DATE - 20, TRUE),
('PSN-006', '3273010000000006', '0001234567892', 'Siti Nurhaliza', 'Perempuan', DATE '2001-02-14', 'Jl. Buah Batu No. 56, Bandung',  '085233112233', CURRENT_DATE - 10, TRUE);

-- ---------------------------------------------------------------------
-- Kunjungan lama (riwayat). Nomor antrean diisi otomatis oleh trigger.
-- ---------------------------------------------------------------------
INSERT INTO pemeriksaan (id_pemeriksaan, id_pasien, id_dokter, id_layanan, id_petugas, tanggal_pemeriksaan, keluhan, durasi_keluhan_awal, jenis_penjamin, status) VALUES
('PMR-001', 'PSN-001', 'DKT-003', 'LYN-001', 'PTG-002', CURRENT_DATE - 3, 'Demam dan batuk, nafsu makan menurun', '3 hari',   'NON_BPJS', 'SELESAI'),
('PMR-002', 'PSN-003', 'DKT-003', 'LYN-001', 'PTG-002', CURRENT_DATE - 3, 'Sakit kepala',                          '2 hari',   'NON_BPJS', 'DIPERIKSA'),
('PMR-003', 'PSN-002', 'DKT-001', 'LYN-003', 'PTG-002', CURRENT_DATE - 2, 'Nyeri ulu hati dan mual',               '1 minggu', 'NON_BPJS', 'DIPERIKSA'),
('PMR-004', 'PSN-004', 'DKT-004', 'LYN-002', 'PTG-002', CURRENT_DATE - 2, 'Nyeri gigi geraham',                    '4 hari',   'NON_BPJS', 'SELESAI');

INSERT INTO rekam_medis (id_rekam_medis, id_pemeriksaan, tensi_sistolik, tensi_diastolik, tinggi_badan_cm, berat_badan_kg,
                         durasi_keluhan, hasil_pemeriksaan, diagnosis, catatan, resep, obat_diberikan) VALUES
('RM-001', 'PMR-001', 120, 80, 170.0, 65.0, '3 hari',   'Tenggorokan hiperemis, suhu 38.2 C, paru bersih.',
 'Faringitis akut dan febris hari ke-3', 'Kontrol ulang bila demam tidak turun dalam 3 hari.',
 'Paracetamol 500 mg 3x1, Amoxicillin 500 mg 3x1', 'Paracetamol 500 mg 10 tablet, Amoxicillin 500 mg 15 kapsul'),
('RM-002', 'PMR-002', 110, 70, 172.0, 70.0, '2 hari',   'Tidak ditemukan tanda neurologis fokal.',
 'Cephalgia ringan', 'Istirahat cukup.',
 'Paracetamol 500 mg, 3 x 1', 'Paracetamol 500 mg 9 tablet'),
('RM-003', 'PMR-003', 130, 85, 165.0, 72.0, '1 minggu', 'Nyeri tekan epigastrium.',
 'Dispepsia fungsional', 'Hindari makanan pedas dan asam.',
 'Antasida sirup, Omeprazole 20 mg', 'Antasida sirup 1 botol, Omeprazole 20 mg 14 kapsul'),
('RM-004', 'PMR-004', 120, 80, 160.0, 55.0, '4 hari',   'Karies profunda gigi 36.',
 'Pulpitis reversibel', 'Penambalan sementara, kontrol 1 minggu.',
 'Asam mefenamat 500 mg 3x1', 'Asam mefenamat 500 mg 9 tablet');

-- Surat: dibuat saat kunjungan DIPERIKSA, lalu diserahkan petugas.
INSERT INTO surat (id_surat, id_pemeriksaan, jenis, isi, jumlah_hari, tujuan_rujukan, tanggal_dibuat) VALUES
('SRT-001', 'PMR-002', 'IZIN',    'Pasien memerlukan istirahat karena sakit.',        2,    NULL,                                         CURRENT_DATE - 3),
('SRT-002', 'PMR-003', 'RUJUKAN', 'Rujukan untuk pemeriksaan lanjutan endoskopi.',    NULL, 'RSUD Kota Bandung - Poli Penyakit Dalam',    CURRENT_DATE - 2);

UPDATE surat SET status = 'DISERAHKAN', diserahkan_oleh = 'petugas' WHERE id_surat IN ('SRT-001', 'SRT-002');

UPDATE pemeriksaan SET status = 'SELESAI' WHERE id_pemeriksaan IN ('PMR-002', 'PMR-003');

INSERT INTO pembayaran (id_pembayaran, id_pemeriksaan, tanggal_pembayaran, biaya_tambahan, total_biaya, metode_pembayaran, status_pembayaran) VALUES
('BYR-001', 'PMR-001', CURRENT_DATE - 3,      0,  75000, 'TUNAI',    'LUNAS'),
('BYR-002', 'PMR-002', CURRENT_DATE - 3,  10000,  85000, 'TRANSFER', 'LUNAS'),
('BYR-003', 'PMR-003', CURRENT_DATE - 2,  25000, 225000, 'TUNAI',    'BELUM_LUNAS'),   -- 150000 + 50000 konsultasi spesialis + 25000
('BYR-004', 'PMR-004', CURRENT_DATE - 2,      0,  90000, 'TUNAI',    'LUNAS');

-- Pasien dan dokter yang sudah punya riwayat dinonaktifkan (contoh data nonaktif, BR-11).
UPDATE pasien SET status_aktif = FALSE WHERE id_pasien = 'PSN-004';
UPDATE dokter SET status_aktif = FALSE WHERE id_dokter = 'DKT-004';

-- ---------------------------------------------------------------------
-- Kunjungan hari ini (satu di tiap tahap, satu dibatalkan)
-- ---------------------------------------------------------------------
INSERT INTO pemeriksaan (id_pemeriksaan, id_pasien, id_dokter, id_layanan, id_petugas, tanggal_pemeriksaan, keluhan, durasi_keluhan_awal, jenis_penjamin, status, jumlah_panggilan, alasan_batal, dibatalkan_oleh) VALUES
('PMR-005', 'PSN-005', 'DKT-003', 'LYN-001', 'PTG-002', CURRENT_DATE, 'Batuk pilek', '5 hari', 'NON_BPJS', 'DIBATALKAN', 3,
 'Pasien tidak hadir setelah dipanggil 3 kali.', 'petugas');

INSERT INTO pemeriksaan (id_pemeriksaan, id_pasien, id_dokter, id_layanan, id_petugas, tanggal_pemeriksaan, keluhan, durasi_keluhan_awal, jenis_penjamin, status) VALUES
('PMR-006', 'PSN-003', 'DKT-006', 'LYN-001', 'PTG-002', CURRENT_DATE, 'Sakit kepala',      '2 hari', 'NON_BPJS', 'DIPERIKSA'),
('PMR-007', 'PSN-001', 'DKT-006', 'LYN-001', 'PTG-002', CURRENT_DATE, 'Demam dan batuk',   '3 hari', 'BPJS',     'TTV_SELESAI'),
('PMR-008', 'PSN-006', 'DKT-006', 'LYN-001', 'PTG-002', CURRENT_DATE, 'Pusing',            '1 hari', 'BPJS',     'TERDAFTAR');

INSERT INTO rekam_medis (id_rekam_medis, id_pemeriksaan, tensi_sistolik, tensi_diastolik, tinggi_badan_cm, berat_badan_kg,
                         durasi_keluhan, hasil_pemeriksaan, diagnosis, catatan, resep, obat_diberikan) VALUES
('RM-005', 'PMR-006', 118, 76, 172.0, 70.0, '2 hari', 'Tidak ada tanda neurologis fokal.', 'Cephalgia ringan', NULL,
 'Paracetamol 500 mg, 3 x 1', NULL),
('RM-006', 'PMR-007', 120, 80, 170.0, 65.0, NULL, NULL, NULL, NULL, NULL, NULL);

INSERT INTO surat (id_surat, id_pemeriksaan, jenis, isi, jumlah_hari, tujuan_rujukan) VALUES
('SRT-003', 'PMR-006', 'IZIN', 'Pasien memerlukan istirahat karena sakit.', 3, NULL);

-- ---------------------------------------------------------------------
-- Sesuaikan sequence dengan data seed supaya ID berikutnya melanjutkan (PSN-007, PMR-009, ...)
-- ---------------------------------------------------------------------
SELECT setval('seq_petugas',     (SELECT MAX(regexp_replace(id_petugas,     '^[A-Z]+-', '')::int) FROM petugas));
SELECT setval('seq_pasien',      (SELECT MAX(regexp_replace(id_pasien,      '^[A-Z]+-', '')::int) FROM pasien));
SELECT setval('seq_dokter',      (SELECT MAX(regexp_replace(id_dokter,      '^[A-Z]+-', '')::int) FROM dokter));
SELECT setval('seq_layanan',     (SELECT MAX(regexp_replace(id_layanan,     '^[A-Z]+-', '')::int) FROM layanan));
SELECT setval('seq_pemeriksaan', (SELECT MAX(regexp_replace(id_pemeriksaan, '^[A-Z]+-', '')::int) FROM pemeriksaan));
SELECT setval('seq_rekam_medis', (SELECT MAX(regexp_replace(id_rekam_medis, '^[A-Z]+-', '')::int) FROM rekam_medis));
SELECT setval('seq_pembayaran',  (SELECT MAX(regexp_replace(id_pembayaran,  '^[A-Z]+-', '')::int) FROM pembayaran));
SELECT setval('seq_surat',       (SELECT MAX(regexp_replace(id_surat,       '^[A-Z]+-', '')::int) FROM surat));

COMMIT;
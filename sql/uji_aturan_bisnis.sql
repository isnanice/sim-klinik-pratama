-- uji_aturan_bisnis.sql
-- Menguji seluruh aturan bisnis, constraint, dan trigger yang dijaga database PostgreSQL (db_klinik_pratama)
-- mencakup Cakupan Anggota 1 (Petugas, Pasien, Surat) serta Anggota 2 & 3 (Pemeriksaan, Rekam Medis, Pembayaran).
-- Jalankan SETELAH schema.sql + seed.sql:  psql -U klinik_user -d db_klinik_pratama -f sql/uji_aturan_bisnis.sql
-- Seluruh skenario dibungkus transaksi dan di-ROLLBACK di akhir, sehingga data seed/utama tidak berubah.

\set ON_ERROR_STOP off
\pset tuples_only on
BEGIN;

CREATE TEMP TABLE hasil (no SERIAL, uji TEXT, status TEXT);

-- Pembantu: menjalankan perintah yang HARUS ditolak database.
CREATE FUNCTION pg_temp.harus_ditolak(nama TEXT, perintah TEXT) RETURNS VOID LANGUAGE plpgsql AS $$
BEGIN
    EXECUTE perintah;
    INSERT INTO hasil(uji, status) VALUES (nama, 'GAGAL (tidak ditolak)');
EXCEPTION WHEN OTHERS THEN
    INSERT INTO hasil(uji, status) VALUES (nama, 'LULUS (ditolak: ' || left(SQLERRM, 70) || ')');
END $$;

-- Pembantu: menjalankan perintah yang HARUS berhasil.
CREATE FUNCTION pg_temp.harus_berhasil(nama TEXT, perintah TEXT) RETURNS VOID LANGUAGE plpgsql AS $$
BEGIN
    EXECUTE perintah;
    INSERT INTO hasil(uji, status) VALUES (nama, 'LULUS');
EXCEPTION WHEN OTHERS THEN
    INSERT INTO hasil(uji, status) VALUES (nama, 'GAGAL (' || left(SQLERRM, 80) || ')');
END $$;

-- =====================================================================
-- BAGIAN A (ANGGOTA 1): PETUGAS, PASIEN, DAN SURAT (13 Skenario)
-- =====================================================================

-- 1. Tabel petugas (uq_petugas_username, chk_petugas_password, chk_petugas_id_dokter, ON DELETE RESTRICT)
SELECT pg_temp.harus_ditolak('petugas: username duplikat ditolak (uq_petugas_username)',
 $q$INSERT INTO petugas (nama, no_telepon, username, password_hash, role, id_dokter, status_aktif)
    VALUES ('Duplikat Admin', '081234567890', 'admin', 'y8AHLucmS3OeYXRSFHr+Bw==:5BMIimWGEOEctPYKiUQ7ADrIMXtDHlEGpzrxB5VL3b0=', 'ADMIN', NULL, TRUE)$q$);

SELECT pg_temp.harus_ditolak('petugas: password polos tanpa salt:hash ditolak (chk_petugas_password)',
 $q$INSERT INTO petugas (nama, no_telepon, username, password_hash, role, id_dokter, status_aktif)
    VALUES ('Petugas Polos', '081234567890', 'ptg_polos', 'password123', 'PETUGAS', NULL, TRUE)$q$);

SELECT pg_temp.harus_ditolak('petugas: role DOKTER tanpa id_dokter ditolak (chk_petugas_id_dokter)',
 $q$INSERT INTO petugas (nama, no_telepon, username, password_hash, role, id_dokter, status_aktif)
    VALUES ('Dokter Tanpa ID', '081234567890', 'dokter_tanpa_id', 'y8AHLucmS3OeYXRSFHr+Bw==:5BMIimWGEOEctPYKiUQ7ADrIMXtDHlEGpzrxB5VL3b0=', 'DOKTER', NULL, TRUE)$q$);

SELECT pg_temp.harus_ditolak('petugas: hapus petugas yang punya riwayat pemeriksaan ditolak (BR-11)',
 $q$DELETE FROM petugas WHERE id_petugas = 'PTG-002'$q$);

-- 2. Tabel pasien (chk_pasien_nik, chk_pasien_bpjs BR-13, chk_pasien_lahir BR-05, ON DELETE RESTRICT BR-11)
SELECT pg_temp.harus_ditolak('pasien: NIK kurang dari 16 digit ditolak (chk_pasien_nik)',
 $q$INSERT INTO pasien (nik, no_bpjs, nama, jenis_kelamin, tanggal_lahir, alamat, no_telepon)
    VALUES ('12345', NULL, 'Pasien NIK Pendek', 'Laki-laki', DATE '1999-01-01', 'Bandung', '081234567890')$q$);

SELECT pg_temp.harus_ditolak('pasien: nomor BPJS bukan 13 digit ditolak (BR-13 / chk_pasien_bpjs)',
 $q$INSERT INTO pasien (nik, no_bpjs, nama, jenis_kelamin, tanggal_lahir, alamat, no_telepon)
    VALUES ('3273019999999991', '123456789', 'Pasien BPJS Salah', 'Perempuan', DATE '1998-05-10', 'Bandung', '081234567890')$q$);

SELECT pg_temp.harus_ditolak('pasien: tanggal lahir di masa depan ditolak (BR-05 / chk_pasien_lahir)',
 $q$INSERT INTO pasien (nik, no_bpjs, nama, jenis_kelamin, tanggal_lahir, alamat, no_telepon)
    VALUES ('3273019999999992', NULL, 'Pasien Masa Depan', 'Laki-laki', CURRENT_DATE + 5, 'Bandung', '081234567890')$q$);

SELECT pg_temp.harus_ditolak('pasien: hapus permanen pasien dengan riwayat pemeriksaan ditolak (BR-11)',
 $q$DELETE FROM pasien WHERE id_pasien = 'PSN-001'$q$);

-- 3. Tabel surat & Trigger BR-17 (chk_srt_isian, trg_surat_insert, trg_surat_kunci)
SELECT pg_temp.harus_ditolak('surat: surat IZIN dengan jumlah_hari < 1 ditolak (BR-17 / chk_srt_isian)',
 $q$INSERT INTO surat (id_pemeriksaan, jenis, isi, jumlah_hari, tujuan_rujukan)
    VALUES ('PMR-006', 'IZIN', 'Istirahat medis', 0, NULL)$q$);

SELECT pg_temp.harus_ditolak('surat: surat RUJUKAN dengan tujuan kosong ditolak (BR-17 / chk_srt_isian)',
 $q$INSERT INTO surat (id_pemeriksaan, jenis, isi, jumlah_hari, tujuan_rujukan)
    VALUES ('PMR-006', 'RUJUKAN', 'Rujukan poli penyakit dalam', NULL, '   ')$q$);

SELECT pg_temp.harus_ditolak('surat: buat surat saat kunjungan TERDAFTAR ditolak (BR-17 / trg_surat_insert)',
 $q$INSERT INTO surat (id_pemeriksaan, jenis, isi, jumlah_hari, tujuan_rujukan)
    VALUES ('PMR-008', 'IZIN', 'Istirahat 2 hari', 2, NULL)$q$);

SELECT pg_temp.harus_ditolak('surat: ubah surat berstatus DISERAHKAN ditolak (BR-17 / trg_surat_kunci)',
 $q$UPDATE surat SET isi = 'Diubah setelah diserahkan' WHERE id_surat = 'SRT-001'$q$);

SELECT pg_temp.harus_ditolak('surat: hapus surat berstatus DISERAHKAN ditolak (BR-17 / trg_surat_kunci)',
 $q$DELETE FROM surat WHERE id_surat = 'SRT-001'$q$);


-- =====================================================================
-- BAGIAN B (ANGGOTA 2 & 3): PENDAFTARAN, STATUS, REKAM MEDIS, PEMBAYARAN
-- =====================================================================

-- ---------- Pendaftaran: nomor antrean otomatis (BR-17) dan BPJS (BR-16)
SELECT pg_temp.harus_berhasil('daftar kunjungan dengan no_antrean NULL (trigger mengisi)',
 $q$INSERT INTO pemeriksaan(id_pasien,id_dokter,id_layanan,id_petugas,keluhan,durasi_keluhan_awal,jenis_penjamin,no_antrean)
    VALUES ('PSN-002','DKT-001','LYN-001','PTG-002','Pusing','2 hari','NON_BPJS',NULL)$q$);
SELECT pg_temp.harus_berhasil('no_antrean dokter berurutan per hari',
 $q$DO $d$ BEGIN
      IF (SELECT no_antrean FROM pemeriksaan WHERE id_dokter='DKT-001' AND tanggal_pemeriksaan=CURRENT_DATE
          ORDER BY id_pemeriksaan DESC LIMIT 1) IS NULL THEN RAISE EXCEPTION 'antrean kosong'; END IF; END $d$$q$);
SELECT pg_temp.harus_ditolak('BPJS tanpa nomor BPJS ditolak (BR-16)',
 $q$INSERT INTO pemeriksaan(id_pasien,id_dokter,id_layanan,id_petugas,keluhan,durasi_keluhan_awal,jenis_penjamin,no_antrean)
    VALUES ('PSN-002','DKT-001','LYN-001','PTG-002','Batuk','1 hari','BPJS',NULL)$q$);
SELECT pg_temp.harus_ditolak('pasien nonaktif tidak dapat didaftarkan (BR-07)',
 $q$INSERT INTO pemeriksaan(id_pasien,id_dokter,id_layanan,id_petugas,keluhan,durasi_keluhan_awal,jenis_penjamin,no_antrean)
    VALUES ('PSN-004','DKT-001','LYN-001','PTG-002','Batuk','1 hari','NON_BPJS',NULL)$q$);
SELECT pg_temp.harus_ditolak('dokter nonaktif tidak dapat dipilih (BR-07)',
 $q$INSERT INTO pemeriksaan(id_pasien,id_dokter,id_layanan,id_petugas,keluhan,durasi_keluhan_awal,jenis_penjamin,no_antrean)
    VALUES ('PSN-002','DKT-004','LYN-001','PTG-002','Batuk','1 hari','NON_BPJS',NULL)$q$);
SELECT pg_temp.harus_ditolak('layanan nonaktif tidak dapat dipilih (BR-07)',
 $q$INSERT INTO pemeriksaan(id_pasien,id_dokter,id_layanan,id_petugas,keluhan,durasi_keluhan_awal,jenis_penjamin,no_antrean)
    VALUES ('PSN-002','DKT-001','LYN-005','PTG-002','Batuk','1 hari','NON_BPJS',NULL)$q$);

-- ---------- Alur status (BR-08): PMR-008 berstatus TERDAFTAR
SELECT pg_temp.harus_ditolak('TERDAFTAR langsung ke DIPERIKSA ditolak (lompat)',
 $q$UPDATE pemeriksaan SET status='DIPERIKSA' WHERE id_pemeriksaan='PMR-008'$q$);
SELECT pg_temp.harus_ditolak('TERDAFTAR langsung ke SELESAI ditolak (lompat)',
 $q$UPDATE pemeriksaan SET status='SELESAI' WHERE id_pemeriksaan='PMR-008'$q$);
SELECT pg_temp.harus_ditolak('SELESAI tidak dapat mundur',
 $q$UPDATE pemeriksaan SET status='DIPERIKSA' WHERE id_pemeriksaan='PMR-001'$q$);
SELECT pg_temp.harus_berhasil('TERDAFTAR ke TTV_SELESAI diterima',
 $q$UPDATE pemeriksaan SET status='TTV_SELESAI' WHERE id_pemeriksaan='PMR-008'$q$);

-- ---------- Pembatalan (BR-14, BR-15)
SELECT pg_temp.harus_ditolak('batal tanpa alasan ditolak',
 $q$UPDATE pemeriksaan SET status='DIBATALKAN' WHERE id_pemeriksaan='PMR-007'$q$);
SELECT pg_temp.harus_ditolak('batal sebelum 3 kali dipanggil ditolak',
 $q$UPDATE pemeriksaan SET status='DIBATALKAN', alasan_batal='tidak hadir', dibatalkan_oleh='petugas'
    WHERE id_pemeriksaan='PMR-007'$q$);
SELECT pg_temp.harus_berhasil('dipanggil 3 kali lalu batal diterima',
 $q$UPDATE pemeriksaan SET jumlah_panggilan=3 WHERE id_pemeriksaan='PMR-007';
    UPDATE pemeriksaan SET status='DIBATALKAN', alasan_batal='tidak hadir', dibatalkan_oleh='petugas'
    WHERE id_pemeriksaan='PMR-007'$q$);
SELECT pg_temp.harus_ditolak('kunjungan DIBATALKAN tidak dapat diubah lagi',
 $q$UPDATE pemeriksaan SET status='TERDAFTAR', alasan_batal=NULL, dibatalkan_oleh=NULL WHERE id_pemeriksaan='PMR-007'$q$);

-- ---------- Rekam medis (BR-09): PMR-006 berstatus DIPERIKSA, PMR-008 sekarang TTV_SELESAI
SELECT pg_temp.harus_ditolak('TTV dengan sistolik <= diastolik ditolak',
 $q$INSERT INTO rekam_medis(id_pemeriksaan,tensi_sistolik,tensi_diastolik,tinggi_badan_cm,berat_badan_kg)
    VALUES ('PMR-008',80,120,170,60)$q$);
SELECT pg_temp.harus_ditolak('berat badan 0 ditolak',
 $q$INSERT INTO rekam_medis(id_pemeriksaan,tensi_sistolik,tensi_diastolik,tinggi_badan_cm,berat_badan_kg)
    VALUES ('PMR-008',120,80,170,0)$q$);
SELECT pg_temp.harus_ditolak('satu rekam medis per kunjungan',
 $q$INSERT INTO rekam_medis(id_pemeriksaan,tensi_sistolik,tensi_diastolik,tinggi_badan_cm,berat_badan_kg)
    VALUES ('PMR-006',120,80,170,60)$q$);

-- ---------- Pembayaran (BR-10, BR-12)
-- PMR-T01: kunjungan SELESAI yang belum dibayar, dibuat bertahap di dalam transaksi uji
INSERT INTO pemeriksaan(id_pemeriksaan,id_pasien,id_dokter,id_layanan,id_petugas,keluhan,durasi_keluhan_awal,no_antrean)
    VALUES ('PMR-T01','PSN-002','DKT-002','LYN-001','PTG-002','Uji bayar','1 hari',NULL);
UPDATE pemeriksaan SET status='TTV_SELESAI' WHERE id_pemeriksaan='PMR-T01';
UPDATE pemeriksaan SET status='DIPERIKSA'   WHERE id_pemeriksaan='PMR-T01';
UPDATE pemeriksaan SET status='SELESAI'     WHERE id_pemeriksaan='PMR-T01';
SELECT pg_temp.harus_ditolak('bayar kunjungan belum SELESAI ditolak',
 $q$INSERT INTO pembayaran(id_pemeriksaan,biaya_tambahan,total_biaya,metode_pembayaran,status_pembayaran)
    VALUES ('PMR-006',0,50000,'TUNAI','LUNAS')$q$);
SELECT pg_temp.harus_ditolak('bayar kunjungan DIBATALKAN ditolak',
 $q$INSERT INTO pembayaran(id_pemeriksaan,biaya_tambahan,total_biaya,metode_pembayaran,status_pembayaran)
    VALUES ('PMR-005',0,50000,'TUNAI','LUNAS')$q$);
SELECT pg_temp.harus_ditolak('biaya tambahan negatif ditolak',
 $q$INSERT INTO pembayaran(id_pemeriksaan,biaya_tambahan,total_biaya,metode_pembayaran,status_pembayaran)
    VALUES ('PMR-T01',-1,50000,'TUNAI','LUNAS')$q$);
SELECT pg_temp.harus_ditolak('metode di luar Tunai/Transfer ditolak',
 $q$INSERT INTO pembayaran(id_pemeriksaan,biaya_tambahan,total_biaya,metode_pembayaran,status_pembayaran)
    VALUES ('PMR-T01',0,50000,'KARTU','LUNAS')$q$);
SELECT pg_temp.harus_berhasil('bayar kunjungan SELESAI sebagai BELUM_LUNAS diterima',
 $q$INSERT INTO pembayaran(id_pemeriksaan,biaya_tambahan,total_biaya,metode_pembayaran,status_pembayaran)
    VALUES ('PMR-T01',5000,55000,'TRANSFER','BELUM_LUNAS')$q$);
SELECT pg_temp.harus_berhasil('BELUM_LUNAS dapat diubah menjadi LUNAS',
 $q$UPDATE pembayaran SET status_pembayaran='LUNAS' WHERE id_pemeriksaan='PMR-T01'$q$);
SELECT pg_temp.harus_ditolak('setelah LUNAS, status terkunci',
 $q$UPDATE pembayaran SET status_pembayaran='BELUM_LUNAS' WHERE id_pemeriksaan='PMR-T01'$q$);

-- pembayaran ganda dan kunci LUNAS memakai pembayaran yang sudah ada di seed
SELECT pg_temp.harus_ditolak('pembayaran kedua untuk kunjungan yang sama ditolak',
 $q$INSERT INTO pembayaran(id_pemeriksaan,biaya_tambahan,total_biaya,metode_pembayaran,status_pembayaran)
    SELECT id_pemeriksaan,0,10000,'TUNAI','LUNAS' FROM pembayaran LIMIT 1$q$);
SELECT pg_temp.harus_ditolak('pembayaran LUNAS tidak dapat diubah',
 $q$UPDATE pembayaran SET status_pembayaran='BELUM_LUNAS' WHERE status_pembayaran='LUNAS'$q$);
SELECT pg_temp.harus_ditolak('pembayaran LUNAS tidak dapat dihapus',
 $q$DELETE FROM pembayaran WHERE status_pembayaran='LUNAS'$q$);

SELECT format('%2s. %s: %s', no, status, uji) FROM hasil ORDER BY no;
SELECT format('Ringkasan: %s lulus, %s gagal', count(*) FILTER (WHERE status LIKE 'LULUS%'),
              count(*) FILTER (WHERE status LIKE 'GAGAL%')) FROM hasil;

ROLLBACK;

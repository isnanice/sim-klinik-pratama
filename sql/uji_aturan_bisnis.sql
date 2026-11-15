-- uji_aturan_bisnis.sql
-- Menguji aturan bisnis yang dijaga database (PRD bagian 12: pengujian 6, 7, 8, 10).
-- Jalankan SETELAH schema.sql + seed.sql:  psql -U klinik_user -d db_klinik_pratama -f sql/uji_aturan_bisnis.sql
-- Seluruh skenario dibungkus transaksi dan di-ROLLBACK, jadi data tidak berubah.
-- Tiap baris hasil diawali LULUS atau GAGAL.

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

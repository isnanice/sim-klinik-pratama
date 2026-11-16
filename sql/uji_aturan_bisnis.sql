-- uji_aturan_bisnis.sql : Pengujian Constraint & Trigger PostgreSQL Cakupan Anggota 1 (Petugas, Pasien, Surat)
-- Database : db_klinik_pratama
-- Jalankan : psql -U klinik_user -d db_klinik_pratama -f sql/uji_aturan_bisnis.sql
-- Catatan  : Setiap blok pengujian menggunakan SAVEPOINT dan ROLLBACK TO SAVEPOINT
--            agar data seed/utama tidak rusak setelah pengujian selesai.

BEGIN;

-- =====================================================================
-- 1. PENGUJIAN TABEL PETUGAS (BR-01, BR-04, Keterkaitan Role DOKTER)
-- =====================================================================

-- 1a. Uji uq_petugas_username (Username duplikat harus ditolak - SQLState 23505)
SAVEPOINT uji_ptg_username_unik;
DO $$
BEGIN
    INSERT INTO petugas (nama, no_telepon, username, password_hash, role, id_dokter, status_aktif)
    VALUES ('Duplikat Admin', '081234567890', 'admin', 'y8AHLucmS3OeYXRSFHr+Bw==:5BMIimWGEOEctPYKiUQ7ADrIMXtDHlEGpzrxB5VL3b0=', 'ADMIN', NULL, TRUE);
    RAISE EXCEPTION 'GAGAL UJI: Username duplikat seharusnya ditolak!';
EXCEPTION
    WHEN unique_violation THEN
        RAISE NOTICE '[LULUS] uq_petugas_username menolak username duplikat (SQLState 23505).';
END $$;
ROLLBACK TO SAVEPOINT uji_ptg_username_unik;

-- 1b. Uji chk_petugas_password (Password polos tanpa format salt:hash harus ditolak - SQLState 23514)
SAVEPOINT uji_ptg_password_polos;
DO $$
BEGIN
    INSERT INTO petugas (nama, no_telepon, username, password_hash, role, id_dokter, status_aktif)
    VALUES ('Petugas Polos', '081234567890', 'ptg_polos', 'password123', 'PETUGAS', NULL, TRUE);
    RAISE EXCEPTION 'GAGAL UJI: Password polos tanpa salt:hash seharusnya ditolak!';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE '[LULUS] chk_petugas_password menolak password polos tanpa format salt:hash.';
END $$;
ROLLBACK TO SAVEPOINT uji_ptg_password_polos;

-- 1c. Uji chk_petugas_id_dokter (Role DOKTER wajib memiliki id_dokter, non-DOKTER wajib NULL)
SAVEPOINT uji_ptg_role_dokter;
DO $$
BEGIN
    INSERT INTO petugas (nama, no_telepon, username, password_hash, role, id_dokter, status_aktif)
    VALUES ('Dokter Tanpa ID', '081234567890', 'dokter_tanpa_id', 'y8AHLucmS3OeYXRSFHr+Bw==:5BMIimWGEOEctPYKiUQ7ADrIMXtDHlEGpzrxB5VL3b0=', 'DOKTER', NULL, TRUE);
    RAISE EXCEPTION 'GAGAL UJI: Role DOKTER tanpa id_dokter seharusnya ditolak!';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE '[LULUS] chk_petugas_id_dokter menolak role DOKTER dengan id_dokter NULL.';
END $$;
ROLLBACK TO SAVEPOINT uji_ptg_role_dokter;

-- 1d. Uji ON DELETE RESTRICT pada petugas yang sudah memiliki riwayat pemeriksaan (BR-11 - SQLState 23503 / 23001)
SAVEPOINT uji_ptg_hapus_restrict;
DO $$
BEGIN
    DELETE FROM petugas WHERE id_petugas = 'PTG-002';
    RAISE EXCEPTION 'GAGAL UJI: Petugas dengan riwayat pemeriksaan seharusnya tidak bisa dihapus!';
EXCEPTION
    WHEN foreign_key_violation OR restrict_violation THEN
        RAISE NOTICE '[LULUS] ON DELETE RESTRICT menolak penghapusan PTG-002 yang memiliki riwayat pemeriksaan (SQLState %).', SQLSTATE;
END $$;
ROLLBACK TO SAVEPOINT uji_ptg_hapus_restrict;


-- =====================================================================
-- 2. PENGUJIAN TABEL PASIEN (BR-03, BR-04, BR-05, BR-11, BR-13)
-- =====================================================================

-- 2a. Uji chk_pasien_nik & uq_pasien_nik (NIK wajib 16 digit angka & unik)
SAVEPOINT uji_psn_nik;
DO $$
BEGIN
    INSERT INTO pasien (nik, no_bpjs, nama, jenis_kelamin, tanggal_lahir, alamat, no_telepon)
    VALUES ('12345', NULL, 'Pasien NIK Pendek', 'Laki-laki', DATE '1999-01-01', 'Bandung', '081234567890');
    RAISE EXCEPTION 'GAGAL UJI: NIK kurang dari 16 digit seharusnya ditolak!';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE '[LULUS] chk_pasien_nik menolak NIK yang bukan 16 digit angka.';
END $$;
ROLLBACK TO SAVEPOINT uji_psn_nik;

-- 2b. Uji chk_pasien_bpjs (BR-13: No. BPJS opsional/NULL, tetapi jika diisi wajib 13 digit angka)
SAVEPOINT uji_psn_bpjs;
DO $$
BEGIN
    INSERT INTO pasien (nik, no_bpjs, nama, jenis_kelamin, tanggal_lahir, alamat, no_telepon)
    VALUES ('3273019999999991', '123456789', 'Pasien BPJS Salah', 'Perempuan', DATE '1998-05-10', 'Bandung', '081234567890');
    RAISE EXCEPTION 'GAGAL UJI: Nomor BPJS 9 digit seharusnya ditolak!';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE '[LULUS] BR-13 (chk_pasien_bpjs) menolak nomor BPJS yang bukan 13 digit angka.';
END $$;
ROLLBACK TO SAVEPOINT uji_psn_bpjs;

-- 2c. Uji chk_pasien_lahir (BR-05: Tanggal lahir tidak boleh di masa depan)
SAVEPOINT uji_psn_tgl_lahir;
DO $$
BEGIN
    INSERT INTO pasien (nik, no_bpjs, nama, jenis_kelamin, tanggal_lahir, alamat, no_telepon)
    VALUES ('3273019999999992', NULL, 'Pasien Masa Depan', 'Laki-laki', CURRENT_DATE + 5, 'Bandung', '081234567890');
    RAISE EXCEPTION 'GAGAL UJI: Tanggal lahir di masa depan seharusnya ditolak!';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE '[LULUS] BR-05 (chk_pasien_lahir) menolak tanggal lahir di masa depan.';
END $$;
ROLLBACK TO SAVEPOINT uji_psn_tgl_lahir;

-- 2d. Uji ON DELETE RESTRICT pada pasien yang memiliki riwayat pemeriksaan (BR-11 - SQLState 23503 / 23001)
SAVEPOINT uji_psn_hapus_restrict;
DO $$
BEGIN
    DELETE FROM pasien WHERE id_pasien = 'PSN-001';
    RAISE EXCEPTION 'GAGAL UJI: Pasien PSN-001 yang memiliki riwayat pemeriksaan seharusnya tidak bisa dihapus permanen!';
EXCEPTION
    WHEN foreign_key_violation OR restrict_violation THEN
        RAISE NOTICE '[LULUS] BR-11 (ON DELETE RESTRICT) menolak hapus permanen PSN-001 (SQLState %).', SQLSTATE;
END $$;
ROLLBACK TO SAVEPOINT uji_psn_hapus_restrict;


-- =====================================================================
-- 3. PENGUJIAN TABEL SURAT & TRIGGER BR-17
-- =====================================================================

-- 3a. Uji chk_srt_isian (BR-17: Surat IZIN wajib jumlah_hari >= 1 dan tujuan_rujukan NULL)
SAVEPOINT uji_srt_izin_invalid;
DO $$
BEGIN
    INSERT INTO surat (id_pemeriksaan, jenis, isi, jumlah_hari, tujuan_rujukan)
    VALUES ('PMR-006', 'IZIN', 'Istirahat medis', 0, NULL);
    RAISE EXCEPTION 'GAGAL UJI: Surat IZIN dengan jumlah_hari = 0 seharusnya ditolak!';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE '[LULUS] BR-17 (chk_srt_isian) menolak surat IZIN dengan jumlah_hari < 1.';
END $$;
ROLLBACK TO SAVEPOINT uji_srt_izin_invalid;

-- 3b. Uji chk_srt_isian (BR-17: Surat RUJUKAN wajib tujuan_rujukan tidak kosong dan jumlah_hari NULL)
SAVEPOINT uji_srt_rujukan_invalid;
DO $$
BEGIN
    INSERT INTO surat (id_pemeriksaan, jenis, isi, jumlah_hari, tujuan_rujukan)
    VALUES ('PMR-006', 'RUJUKAN', 'Rujukan poli penyakit dalam', NULL, '   ');
    RAISE EXCEPTION 'GAGAL UJI: Surat RUJUKAN dengan tujuan kosong seharusnya ditolak!';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE '[LULUS] BR-17 (chk_srt_isian) menolak surat RUJUKAN tanpa tujuan_rujukan.';
END $$;
ROLLBACK TO SAVEPOINT uji_srt_rujukan_invalid;

-- 3c. Uji trg_surat_insert (BR-17: Surat tidak boleh dibuat untuk kunjungan berstatus TERDAFTAR / SELESAI / DIBATALKAN)
SAVEPOINT uji_srt_status_kunjungan;
DO $$
BEGIN
    -- PMR-008 pada seed berstatus TERDAFTAR
    INSERT INTO surat (id_pemeriksaan, jenis, isi, jumlah_hari, tujuan_rujukan)
    VALUES ('PMR-008', 'IZIN', 'Istirahat 2 hari', 2, NULL);
    RAISE EXCEPTION 'GAGAL UJI: Pembuatan surat untuk kunjungan TERDAFTAR seharusnya ditolak!';
EXCEPTION
    WHEN raise_exception THEN
        RAISE NOTICE '[LULUS] BR-17 (trg_surat_insert) menolak pembuatan surat pada kunjungan berstatus TERDAFTAR: %', SQLERRM;
END $$;
ROLLBACK TO SAVEPOINT uji_srt_status_kunjungan;

-- 3d. Uji trg_surat_kunci (BR-17: Surat yang sudah DISERAHKAN tidak dapat diubah atau dihapus)
SAVEPOINT uji_srt_kunci_diserahkan;
DO $$
BEGIN
    -- SRT-001 pada seed berstatus DISERAHKAN
    UPDATE surat SET isi = 'Diubah setelah diserahkan' WHERE id_surat = 'SRT-001';
    RAISE EXCEPTION 'GAGAL UJI: Surat berstatus DISERAHKAN seharusnya terkunci dari UPDATE!';
EXCEPTION
    WHEN raise_exception THEN
        RAISE NOTICE '[LULUS] BR-17 (trg_surat_kunci) menolak UPDATE pada surat berstatus DISERAHKAN: %', SQLERRM;
END $$;
ROLLBACK TO SAVEPOINT uji_srt_kunci_diserahkan;

SAVEPOINT uji_srt_hapus_diserahkan;
DO $$
BEGIN
    DELETE FROM surat WHERE id_surat = 'SRT-001';
    RAISE EXCEPTION 'GAGAL UJI: Surat berstatus DISERAHKAN seharusnya terkunci dari DELETE!';
EXCEPTION
    WHEN raise_exception THEN
        RAISE NOTICE '[LULUS] BR-17 (trg_surat_kunci) menolak DELETE pada surat berstatus DISERAHKAN: %', SQLERRM;
END $$;
ROLLBACK TO SAVEPOINT uji_srt_hapus_diserahkan;

ROLLBACK;

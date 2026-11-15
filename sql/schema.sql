-- schema.sql : Sistem Informasi Manajemen Klinik Pratama (PRD v1.2, bagian 7 dan 9)
-- Database : db_klinik_pratama (PostgreSQL 12 ke atas)
-- Jalankan : psql -U klinik_user -d db_klinik_pratama -v ON_ERROR_STOP=1 -f sql/schema.sql
-- Seluruh skrip berjalan dalam satu transaksi: jika ada yang gagal, tidak ada yang tersimpan.

BEGIN;

-- =====================================================================
-- 1. SEQUENCE DAN FUNGSI PEMBUAT ID  (PSN-001, DKT-001, dan seterusnya)
-- =====================================================================
CREATE SEQUENCE seq_petugas     START 1;
CREATE SEQUENCE seq_pasien      START 1;
CREATE SEQUENCE seq_dokter      START 1;
CREATE SEQUENCE seq_layanan     START 1;
CREATE SEQUENCE seq_pemeriksaan START 1;
CREATE SEQUENCE seq_rekam_medis START 1;
CREATE SEQUENCE seq_pembayaran  START 1;
CREATE SEQUENCE seq_surat       START 1;

-- Angka diberi nol di depan sampai minimal 'lebar' digit, tanpa memotong angka yang lebih panjang.
CREATE FUNCTION fn_pad(n BIGINT, lebar INTEGER DEFAULT 3) RETURNS TEXT
LANGUAGE sql IMMUTABLE AS $$
    SELECT CASE WHEN length(n::text) >= lebar THEN n::text ELSE lpad(n::text, lebar, '0') END
$$;

CREATE FUNCTION fn_id(prefix TEXT, n BIGINT) RETURNS TEXT
LANGUAGE sql IMMUTABLE AS $$
    SELECT prefix || '-' || fn_pad(n, 3)
$$;

-- =====================================================================
-- 2. TABEL MASTER
-- =====================================================================
CREATE TABLE dokter (
    id_dokter         VARCHAR(12)  PRIMARY KEY DEFAULT fn_id('DKT', nextval('seq_dokter')),
    nama              VARCHAR(100) NOT NULL,
    jenis_kelamin     VARCHAR(10)  NOT NULL,
    alamat            TEXT,
    no_telepon        VARCHAR(15)  NOT NULL,
    nomor_registrasi  VARCHAR(50)  NOT NULL,
    bidang_pelayanan  VARCHAR(50)  NOT NULL,
    status_aktif      BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_dokter_nama       CHECK (btrim(nama) <> ''),
    CONSTRAINT chk_dokter_kelamin    CHECK (jenis_kelamin IN ('Laki-laki', 'Perempuan')),
    CONSTRAINT chk_dokter_telepon    CHECK (no_telepon ~ '^(08|\+628)[0-9]{8,11}$'),
    CONSTRAINT chk_dokter_no_reg     CHECK (btrim(nomor_registrasi) <> ''),
    CONSTRAINT chk_dokter_bidang     CHECK (btrim(bidang_pelayanan) <> ''),
    CONSTRAINT uq_dokter_no_reg      UNIQUE (nomor_registrasi)
);

CREATE TABLE petugas (
    id_petugas     VARCHAR(12)  PRIMARY KEY DEFAULT fn_id('PTG', nextval('seq_petugas')),
    nama           VARCHAR(100) NOT NULL,
    alamat         TEXT,
    no_telepon     VARCHAR(15)  NOT NULL,
    username       VARCHAR(50)  NOT NULL,
    password_hash  VARCHAR(200) NOT NULL,
    role           VARCHAR(10)  NOT NULL,
    id_dokter      VARCHAR(12)  REFERENCES dokter (id_dokter) ON DELETE RESTRICT,
    status_aktif   BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_petugas_nama      CHECK (btrim(nama) <> ''),
    CONSTRAINT chk_petugas_telepon   CHECK (no_telepon ~ '^(08|\+628)[0-9]{8,11}$'),
    CONSTRAINT chk_petugas_username  CHECK (username ~ '^[A-Za-z0-9._-]{3,50}$'),
    -- format hasil PasswordUtil: Base64(salt) ':' Base64(SHA-256). Mencegah password polos tersimpan.
    CONSTRAINT chk_petugas_password  CHECK (password_hash ~ '^[A-Za-z0-9+/=]+:[A-Za-z0-9+/=]+$'),
    CONSTRAINT chk_petugas_role      CHECK (role IN ('ADMIN', 'PETUGAS', 'DOKTER')),
    -- id_dokter hanya terisi untuk role DOKTER, dan wajib terisi untuk role itu
    CONSTRAINT chk_petugas_id_dokter CHECK (
        (role = 'DOKTER' AND id_dokter IS NOT NULL) OR (role <> 'DOKTER' AND id_dokter IS NULL)),
    CONSTRAINT uq_petugas_username   UNIQUE (username),
    CONSTRAINT uq_petugas_id_dokter  UNIQUE (id_dokter)
);

CREATE TABLE pasien (
    id_pasien          VARCHAR(12)  PRIMARY KEY DEFAULT fn_id('PSN', nextval('seq_pasien')),
    nik                VARCHAR(16)  NOT NULL,
    no_bpjs            VARCHAR(13),
    nama               VARCHAR(100) NOT NULL,
    jenis_kelamin      VARCHAR(10)  NOT NULL,
    tanggal_lahir      DATE         NOT NULL,
    alamat             TEXT,
    no_telepon         VARCHAR(15)  NOT NULL,
    tanggal_registrasi DATE         NOT NULL DEFAULT CURRENT_DATE,
    status_aktif       BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_pasien_nik        CHECK (nik ~ '^[0-9]{16}$'),
    CONSTRAINT chk_pasien_bpjs       CHECK (no_bpjs IS NULL OR no_bpjs ~ '^[0-9]{13}$'),
    CONSTRAINT chk_pasien_nama       CHECK (btrim(nama) <> ''),
    CONSTRAINT chk_pasien_kelamin    CHECK (jenis_kelamin IN ('Laki-laki', 'Perempuan')),
    CONSTRAINT chk_pasien_lahir      CHECK (tanggal_lahir <= CURRENT_DATE),
    CONSTRAINT chk_pasien_telepon    CHECK (no_telepon ~ '^(08|\+628)[0-9]{8,11}$'),
    CONSTRAINT uq_pasien_nik         UNIQUE (nik)
);

CREATE TABLE layanan (
    id_layanan    VARCHAR(12)   PRIMARY KEY DEFAULT fn_id('LYN', nextval('seq_layanan')),
    nama_layanan  VARCHAR(100)  NOT NULL,
    jenis_layanan VARCHAR(10)   NOT NULL,
    tarif         NUMERIC(12,2) NOT NULL,
    keterangan    TEXT,
    status_aktif  BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_layanan_nama   CHECK (btrim(nama_layanan) <> ''),
    CONSTRAINT chk_layanan_jenis  CHECK (jenis_layanan IN ('UMUM', 'SPESIALIS')),
    CONSTRAINT chk_layanan_tarif  CHECK (tarif >= 0)
);

-- =====================================================================
-- 3. TABEL TRANSAKSI
-- =====================================================================
CREATE TABLE pemeriksaan (
    id_pemeriksaan      VARCHAR(12) PRIMARY KEY DEFAULT fn_id('PMR', nextval('seq_pemeriksaan')),
    id_pasien           VARCHAR(12) NOT NULL REFERENCES pasien  (id_pasien)  ON DELETE RESTRICT,
    id_dokter           VARCHAR(12) NOT NULL REFERENCES dokter  (id_dokter)  ON DELETE RESTRICT,
    id_layanan          VARCHAR(12) NOT NULL REFERENCES layanan (id_layanan) ON DELETE RESTRICT,
    id_petugas          VARCHAR(12) NOT NULL REFERENCES petugas (id_petugas) ON DELETE RESTRICT,
    tanggal_pemeriksaan DATE        NOT NULL DEFAULT CURRENT_DATE,
    no_antrean          INTEGER     NOT NULL,
    keluhan             TEXT        NOT NULL,
    durasi_keluhan_awal VARCHAR(50) NOT NULL,
    jenis_penjamin      VARCHAR(10) NOT NULL DEFAULT 'NON_BPJS',
    status              VARCHAR(15) NOT NULL DEFAULT 'TERDAFTAR',
    jumlah_panggilan    INTEGER     NOT NULL DEFAULT 0,
    alasan_batal        TEXT,
    dibatalkan_oleh     VARCHAR(50),    -- username pembatal, tanpa FK (bisa petugas, dokter, atau admin)
    CONSTRAINT chk_pmr_antrean    CHECK (no_antrean > 0),
    CONSTRAINT chk_pmr_keluhan    CHECK (btrim(keluhan) <> ''),
    CONSTRAINT chk_pmr_durasi     CHECK (btrim(durasi_keluhan_awal) <> ''),
    CONSTRAINT chk_pmr_penjamin   CHECK (jenis_penjamin IN ('BPJS', 'NON_BPJS')),
    CONSTRAINT chk_pmr_status     CHECK (status IN ('TERDAFTAR', 'TTV_SELESAI', 'DIPERIKSA', 'SELESAI', 'DIBATALKAN')),
    CONSTRAINT chk_pmr_panggilan  CHECK (jumlah_panggilan >= 0),
    -- BR-15: pembatalan wajib beralasan dan tercatat siapa pembatalnya; kolom itu kosong bila tidak batal
    CONSTRAINT chk_pmr_batal      CHECK (
        (status = 'DIBATALKAN' AND alasan_batal IS NOT NULL AND btrim(alasan_batal) <> ''
                               AND dibatalkan_oleh IS NOT NULL AND btrim(dibatalkan_oleh) <> '')
        OR (status <> 'DIBATALKAN' AND alasan_batal IS NULL AND dibatalkan_oleh IS NULL)),
    CONSTRAINT uq_pmr_antrean     UNIQUE (id_dokter, tanggal_pemeriksaan, no_antrean)
);

CREATE TABLE rekam_medis (
    id_rekam_medis   VARCHAR(12)  PRIMARY KEY DEFAULT fn_id('RM', nextval('seq_rekam_medis')),
    id_pemeriksaan   VARCHAR(12)  NOT NULL REFERENCES pemeriksaan (id_pemeriksaan) ON DELETE RESTRICT,
    -- diisi petugas saat TTV disimpan (BR-09)
    tensi_sistolik   INTEGER      NOT NULL,
    tensi_diastolik  INTEGER      NOT NULL,
    tinggi_badan_cm  NUMERIC(5,1) NOT NULL,
    berat_badan_kg   NUMERIC(5,1) NOT NULL,
    -- diisi bertahap: dokter (durasi sampai catatan), farmasi (obat_diberikan)
    durasi_keluhan   VARCHAR(50),
    hasil_pemeriksaan TEXT,
    diagnosis        TEXT,
    catatan          TEXT,
    resep            TEXT,
    obat_diberikan   TEXT,
    CONSTRAINT chk_rm_sistolik    CHECK (tensi_sistolik  BETWEEN 50 AND 300),
    CONSTRAINT chk_rm_diastolik   CHECK (tensi_diastolik BETWEEN 30 AND 200),
    CONSTRAINT chk_rm_tensi       CHECK (tensi_sistolik > tensi_diastolik),
    CONSTRAINT chk_rm_tinggi      CHECK (tinggi_badan_cm BETWEEN 30 AND 250),
    CONSTRAINT chk_rm_berat       CHECK (berat_badan_kg  BETWEEN 1 AND 500),
    CONSTRAINT uq_rm_pemeriksaan  UNIQUE (id_pemeriksaan)
);

CREATE TABLE pembayaran (
    id_pembayaran     VARCHAR(12)   PRIMARY KEY DEFAULT fn_id('BYR', nextval('seq_pembayaran')),
    id_pemeriksaan    VARCHAR(12)   NOT NULL REFERENCES pemeriksaan (id_pemeriksaan) ON DELETE RESTRICT,
    tanggal_pembayaran DATE         NOT NULL DEFAULT CURRENT_DATE,
    biaya_tambahan    NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_biaya       NUMERIC(12,2) NOT NULL,
    metode_pembayaran VARCHAR(10)   NOT NULL,
    status_pembayaran VARCHAR(12)   NOT NULL DEFAULT 'BELUM_LUNAS',
    CONSTRAINT chk_byr_tambahan   CHECK (biaya_tambahan >= 0),
    CONSTRAINT chk_byr_total      CHECK (total_biaya >= 0),
    CONSTRAINT chk_byr_metode     CHECK (metode_pembayaran IN ('TUNAI', 'TRANSFER')),
    CONSTRAINT chk_byr_status     CHECK (status_pembayaran IN ('BELUM_LUNAS', 'LUNAS')),
    CONSTRAINT uq_byr_pemeriksaan UNIQUE (id_pemeriksaan)
);

CREATE TABLE surat (
    id_surat         VARCHAR(12) PRIMARY KEY DEFAULT fn_id('SRT', nextval('seq_surat')),
    id_pemeriksaan   VARCHAR(12) NOT NULL REFERENCES pemeriksaan (id_pemeriksaan) ON DELETE RESTRICT,
    jenis            VARCHAR(10) NOT NULL,
    isi              TEXT        NOT NULL,
    jumlah_hari      INTEGER,
    tujuan_rujukan   VARCHAR(150),
    tanggal_dibuat   DATE        NOT NULL DEFAULT CURRENT_DATE,
    status           VARCHAR(12) NOT NULL DEFAULT 'DITERUSKAN',
    diserahkan_oleh  VARCHAR(50),
    CONSTRAINT chk_srt_jenis   CHECK (jenis IN ('IZIN', 'RUJUKAN')),
    CONSTRAINT chk_srt_isi     CHECK (btrim(isi) <> ''),
    CONSTRAINT chk_srt_status  CHECK (status IN ('DITERUSKAN', 'DISERAHKAN')),
    -- BR-17: izin wajib jumlah hari minimal 1, rujukan wajib tujuan
    CONSTRAINT chk_srt_isian   CHECK (
        (jenis = 'IZIN'    AND jumlah_hari IS NOT NULL AND jumlah_hari >= 1 AND tujuan_rujukan IS NULL)
        OR (jenis = 'RUJUKAN' AND jumlah_hari IS NULL
                              AND tujuan_rujukan IS NOT NULL AND btrim(tujuan_rujukan) <> '')),
    CONSTRAINT chk_srt_serah   CHECK (
        (status = 'DITERUSKAN' AND diserahkan_oleh IS NULL)
        OR (status = 'DISERAHKAN' AND diserahkan_oleh IS NOT NULL AND btrim(diserahkan_oleh) <> ''))
);

-- =====================================================================
-- 4. INDEKS (kolom FK dan kolom yang sering dicari)
-- =====================================================================
CREATE INDEX idx_pasien_nama       ON pasien (lower(nama));
CREATE INDEX idx_dokter_nama       ON dokter (lower(nama));
CREATE INDEX idx_dokter_bidang     ON dokter (lower(bidang_pelayanan));
CREATE INDEX idx_pmr_pasien        ON pemeriksaan (id_pasien);
CREATE INDEX idx_pmr_dokter        ON pemeriksaan (id_dokter);
CREATE INDEX idx_pmr_layanan       ON pemeriksaan (id_layanan);
CREATE INDEX idx_pmr_petugas       ON pemeriksaan (id_petugas);
CREATE INDEX idx_pmr_tgl_status    ON pemeriksaan (tanggal_pemeriksaan, status);
CREATE INDEX idx_srt_pemeriksaan   ON surat (id_pemeriksaan);
CREATE INDEX idx_srt_status        ON surat (status);

-- =====================================================================
-- 5. TRIGGER ATURAN BISNIS (cadangan di database; pengecekan utama tetap di controller)
--    Pesan error diawali kode aturan (BR-xx) agar mudah ditampilkan oleh DAO.
-- =====================================================================

-- Kunjungan baru: BR-07 (pasien, dokter, layanan aktif), BR-16 (BPJS), dan nomor antrean otomatis.
CREATE FUNCTION trg_pemeriksaan_sebelum_insert() RETURNS TRIGGER
LANGUAGE plpgsql AS $$
DECLARE
    v_aktif BOOLEAN;
    v_bpjs  VARCHAR(13);
BEGIN
    SELECT status_aktif, no_bpjs INTO v_aktif, v_bpjs FROM pasien WHERE id_pasien = NEW.id_pasien;
    IF NOT FOUND OR NOT v_aktif THEN
        RAISE EXCEPTION 'BR-07: Pasien harus terdaftar dan berstatus aktif.';
    END IF;

    SELECT status_aktif INTO v_aktif FROM dokter WHERE id_dokter = NEW.id_dokter;
    IF NOT FOUND OR NOT v_aktif THEN
        RAISE EXCEPTION 'BR-07: Dokter harus terdaftar dan berstatus aktif.';
    END IF;

    SELECT status_aktif INTO v_aktif FROM layanan WHERE id_layanan = NEW.id_layanan;
    IF NOT FOUND OR NOT v_aktif THEN
        RAISE EXCEPTION 'BR-07: Layanan harus terdaftar dan berstatus aktif.';
    END IF;

    IF NEW.jenis_penjamin = 'BPJS' AND v_bpjs IS NULL THEN
        RAISE EXCEPTION 'BR-16: Pasien belum memiliki nomor BPJS, pilih jenis penjamin Non-BPJS.';
    END IF;

    -- Nomor antrean poli: urut per dokter per tanggal. Kunci advisory mencegah dua pendaftaran
    -- bersamaan mendapat nomor yang sama (uq_pmr_antrean menjadi pengaman terakhir).
    IF NEW.no_antrean IS NULL THEN
        PERFORM pg_advisory_xact_lock(hashtext(NEW.id_dokter || NEW.tanggal_pemeriksaan::text));
        SELECT COALESCE(MAX(no_antrean), 0) + 1 INTO NEW.no_antrean
          FROM pemeriksaan
         WHERE id_dokter = NEW.id_dokter AND tanggal_pemeriksaan = NEW.tanggal_pemeriksaan;
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_pemeriksaan_insert
    BEFORE INSERT ON pemeriksaan
    FOR EACH ROW EXECUTE FUNCTION trg_pemeriksaan_sebelum_insert();

-- Perubahan status: BR-08 (berurutan, tidak melompat, SELESAI dan DIBATALKAN terkunci) dan BR-14 (reset panggilan).
CREATE FUNCTION trg_pemeriksaan_sebelum_update() RETURNS TRIGGER
LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.status IS DISTINCT FROM OLD.status THEN
        IF NOT (   (OLD.status = 'TERDAFTAR'   AND NEW.status IN ('TTV_SELESAI', 'DIBATALKAN'))
                OR (OLD.status = 'TTV_SELESAI' AND NEW.status IN ('DIPERIKSA',   'DIBATALKAN'))
                OR (OLD.status = 'DIPERIKSA'   AND NEW.status IN ('SELESAI',     'DIBATALKAN'))) THEN
            RAISE EXCEPTION 'BR-08: Status kunjungan tidak boleh berubah dari % ke %.', OLD.status, NEW.status;
        END IF;
        IF NEW.status <> 'DIBATALKAN' THEN
            NEW.jumlah_panggilan := 0;
        END IF;
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_pemeriksaan_update
    BEFORE UPDATE ON pemeriksaan
    FOR EACH ROW EXECUTE FUNCTION trg_pemeriksaan_sebelum_update();

-- Rekam medis: BR-09 (tidak untuk kunjungan DIBATALKAN)
CREATE FUNCTION trg_rekam_medis_sebelum_tulis() RETURNS TRIGGER
LANGUAGE plpgsql AS $$
DECLARE
    v_status VARCHAR(15);
BEGIN
    SELECT status INTO v_status FROM pemeriksaan WHERE id_pemeriksaan = NEW.id_pemeriksaan;
    IF v_status = 'DIBATALKAN' THEN
        RAISE EXCEPTION 'BR-09: Kunjungan yang dibatalkan tidak dapat diisi rekam medis.';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_rekam_medis
    BEFORE INSERT OR UPDATE ON rekam_medis
    FOR EACH ROW EXECUTE FUNCTION trg_rekam_medis_sebelum_tulis();

-- Pembayaran: BR-10 (hanya kunjungan SELESAI, yang LUNAS terkunci)
CREATE FUNCTION trg_pembayaran_sebelum_insert() RETURNS TRIGGER
LANGUAGE plpgsql AS $$
DECLARE
    v_status VARCHAR(15);
BEGIN
    SELECT status INTO v_status FROM pemeriksaan WHERE id_pemeriksaan = NEW.id_pemeriksaan;
    IF v_status IS DISTINCT FROM 'SELESAI' THEN
        RAISE EXCEPTION 'BR-10: Pembayaran hanya untuk kunjungan berstatus SELESAI.';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_pembayaran_insert
    BEFORE INSERT ON pembayaran
    FOR EACH ROW EXECUTE FUNCTION trg_pembayaran_sebelum_insert();

CREATE FUNCTION trg_pembayaran_kunci_lunas() RETURNS TRIGGER
LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.status_pembayaran = 'LUNAS' THEN
        RAISE EXCEPTION 'BR-10: Pembayaran yang sudah LUNAS tidak dapat diubah atau dihapus.';
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_pembayaran_kunci
    BEFORE UPDATE OR DELETE ON pembayaran
    FOR EACH ROW EXECUTE FUNCTION trg_pembayaran_kunci_lunas();

-- Surat: BR-17 (dibuat saat TTV_SELESAI atau DIPERIKSA, status awal DITERUSKAN, DISERAHKAN terkunci)
CREATE FUNCTION trg_surat_sebelum_insert() RETURNS TRIGGER
LANGUAGE plpgsql AS $$
DECLARE
    v_status VARCHAR(15);
BEGIN
    SELECT status INTO v_status FROM pemeriksaan WHERE id_pemeriksaan = NEW.id_pemeriksaan;
    IF v_status IS NULL OR v_status NOT IN ('TTV_SELESAI', 'DIPERIKSA') THEN
        RAISE EXCEPTION 'BR-17: Surat hanya dapat dibuat saat status kunjungan TTV_SELESAI atau DIPERIKSA.';
    END IF;
    IF NEW.status <> 'DITERUSKAN' THEN
        RAISE EXCEPTION 'BR-17: Status awal surat harus DITERUSKAN.';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_surat_insert
    BEFORE INSERT ON surat
    FOR EACH ROW EXECUTE FUNCTION trg_surat_sebelum_insert();

CREATE FUNCTION trg_surat_kunci_diserahkan() RETURNS TRIGGER
LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.status = 'DISERAHKAN' THEN
        RAISE EXCEPTION 'BR-17: Surat yang sudah DISERAHKAN tidak dapat diubah atau dihapus.';
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_surat_kunci
    BEFORE UPDATE OR DELETE ON surat
    FOR EACH ROW EXECUTE FUNCTION trg_surat_kunci_diserahkan();

-- =====================================================================
-- 6. VIEW (memudahkan DAO mengisi JTable tanpa JOIN berulang)
-- =====================================================================
-- Daftar kunjungan dan antrean. kode_antrean = format tampil di wireframe (P-001).
CREATE VIEW v_kunjungan AS
SELECT p.id_pemeriksaan,
       p.tanggal_pemeriksaan,
       p.no_antrean,
       'P-' || fn_pad(p.no_antrean, 3)  AS kode_antrean,
       p.id_pasien,   ps.nama           AS nama_pasien,
       p.id_dokter,   d.nama            AS nama_dokter,
       d.bidang_pelayanan,
       p.id_layanan,  l.nama_layanan,   l.jenis_layanan,
       p.keluhan, p.durasi_keluhan_awal, p.jenis_penjamin,
       p.status, p.jumlah_panggilan, p.alasan_batal, p.dibatalkan_oleh,
       p.id_petugas
  FROM pemeriksaan p
  JOIN pasien  ps ON ps.id_pasien  = p.id_pasien
  JOIN dokter  d  ON d.id_dokter   = p.id_dokter
  JOIN layanan l  ON l.id_layanan  = p.id_layanan;

-- Riwayat pembayaran.
CREATE VIEW v_pembayaran AS
SELECT b.id_pembayaran, b.id_pemeriksaan, b.tanggal_pembayaran,
       ps.id_pasien, ps.nama AS nama_pasien,
       l.nama_layanan, l.jenis_layanan,
       b.biaya_tambahan, b.total_biaya, b.metode_pembayaran, b.status_pembayaran
  FROM pembayaran b
  JOIN pemeriksaan p ON p.id_pemeriksaan = b.id_pemeriksaan
  JOIN pasien  ps ON ps.id_pasien  = p.id_pasien
  JOIN layanan l  ON l.id_layanan  = p.id_layanan;

-- Riwayat rekam medis per pasien (RekamMedisView, tabel "Riwayat per Pasien").
CREATE VIEW v_riwayat_rekam_medis AS
SELECT p.id_pasien, p.id_pemeriksaan, p.tanggal_pemeriksaan,
       d.nama AS nama_dokter,
       r.id_rekam_medis,
       r.tensi_sistolik, r.tensi_diastolik, r.tinggi_badan_cm, r.berat_badan_kg,
       r.durasi_keluhan, r.hasil_pemeriksaan, r.diagnosis, r.catatan, r.resep, r.obat_diberikan,
       p.status
  FROM rekam_medis r
  JOIN pemeriksaan p ON p.id_pemeriksaan = r.id_pemeriksaan
  JOIN dokter d      ON d.id_dokter      = p.id_dokter;

-- Surat masuk untuk petugas dan daftar surat dokter.
CREATE VIEW v_surat AS
SELECT s.id_surat, s.id_pemeriksaan, s.jenis, s.isi, s.jumlah_hari, s.tujuan_rujukan,
       s.tanggal_dibuat, s.status, s.diserahkan_oleh,
       ps.id_pasien, ps.nama AS nama_pasien,
       d.id_dokter,  d.nama  AS nama_dokter
  FROM surat s
  JOIN pemeriksaan p ON p.id_pemeriksaan = s.id_pemeriksaan
  JOIN pasien ps     ON ps.id_pasien     = p.id_pasien
  JOIN dokter d      ON d.id_dokter      = p.id_dokter;

COMMIT;
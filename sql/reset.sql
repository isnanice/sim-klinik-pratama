-- reset.sql
-- Menghapus SEMUA objek di database db_klinik_pratama (tabel, view, fungsi, sequence).
-- Jalankan sebagai klinik_user, lalu jalankan ulang schema.sql dan seed.sql.

DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
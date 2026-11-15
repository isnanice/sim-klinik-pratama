-- 00_buat_database.sql
-- Dijalankan SEKALI sebagai superuser (postgres). Membuat user dan database aplikasi.
-- Ganti password 'klinik123' sebelum dipakai di luar laptop sendiri.

CREATE ROLE klinik_user LOGIN PASSWORD 'klinik123';
CREATE DATABASE db_klinik_pratama OWNER klinik_user;
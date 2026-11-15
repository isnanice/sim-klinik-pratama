@echo off
chcp 65001 >nul
echo Menjalankan Sistem Informasi Manajemen Klinik Pratama...
echo (Pastikan PostgreSQL berjalan dan db.properties sudah dibuat dari db.properties.example)
call mvn -q compile exec:java

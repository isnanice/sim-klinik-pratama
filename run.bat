@echo off
chcp 65001 >nul

echo   Menjalankan Sistem Informasi Manajemen Klinik Pratama
echo   Database: PostgreSQL (db_klinik_pratama)

java -cp "target/classes;lib/postgresql-42.7.4.jar;lib/*;%USERPROFILE%\.m2\repository\org\postgresql\postgresql\42.7.4\postgresql-42.7.4.jar" com.klinik.App
pause

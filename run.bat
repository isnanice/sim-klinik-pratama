@echo off
chcp 65001 >nul
echo Menjalankan Sistem Informasi Manajemen Klinik Pratama...
echo (Pastikan PostgreSQL berjalan dan db.properties sudah dibuat dari db.properties.example)
where mvn >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    call mvn -q compile exec:java
) else (
    java -cp "target/classes;%USERPROFILE%\.m2\repository\org\postgresql\postgresql\42.7.3\postgresql-42.7.3.jar" com.klinik.App
)

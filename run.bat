@echo off
chcp 65001 >nul
echo Menjalankan Sistem Informasi Manajemen Klinik Pratama...
java -cp "target/classes;%USERPROFILE%\.m2\repository\org\postgresql\postgresql\42.7.3\postgresql-42.7.3.jar" com.klinik.App


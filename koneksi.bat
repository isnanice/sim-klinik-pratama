@echo off
chcp 65001 >nul
echo ======================================================================
echo   Menguji Koneksi Database PostgreSQL
echo ======================================================================
java -cp "target/classes;lib/postgresql-42.7.4.jar;lib/*;%USERPROFILE%\.m2\repository\org\postgresql\postgresql\42.7.4\postgresql-42.7.4.jar" com.klinik.test.KoneksiTest
pause


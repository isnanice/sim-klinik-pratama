@echo off
chcp 65001 >nul
echo Menjalankan pengujian aturan bisnis (AturanModelTest) dan uji koneksi PostgreSQL (KoneksiTest)...
where mvn >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    call mvn -q test-compile
)
java -cp "target/classes;target/test-classes;%USERPROFILE%\.m2\repository\org\postgresql\postgresql\42.7.3\postgresql-42.7.3.jar" com.klinik.test.AturanModelTest
java -cp "target/classes;target/test-classes;%USERPROFILE%\.m2\repository\org\postgresql\postgresql\42.7.3\postgresql-42.7.3.jar" com.klinik.test.KoneksiTest
pause

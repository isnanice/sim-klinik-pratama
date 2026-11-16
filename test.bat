@echo off
chcp 65001 >nul
echo Menjalankan pengujian aturan bisnis (tanpa database)...
call mvn -q test-compile
java -cp "target/classes;target/test-classes" com.klinik.test.AturanModelTest
pause

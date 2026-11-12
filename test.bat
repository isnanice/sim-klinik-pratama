@echo off
chcp 65001 >nul
echo Menjalankan Pengujian Alur Pelayanan Sampai Pembayaran...
java -cp "target/classes;target/test-classes;C:\Users\DELL\.m2\repository\org\junit\jupiter\junit-jupiter-api\5.10.2\junit-jupiter-api-5.10.2.jar;C:\Users\DELL\.m2\repository\org\opentest4j\opentest4j\1.3.0\opentest4j-1.3.0.jar;C:\Users\DELL\.m2\repository\org\junit\platform\junit-platform-commons\1.10.2\junit-platform-commons-1.10.2.jar;C:\Users\DELL\.m2\repository\org\apiguardian\apiguardian-api\1.1.2\apiguardian-api-1.1.2.jar" com.klinik.test.TestRunner
pause


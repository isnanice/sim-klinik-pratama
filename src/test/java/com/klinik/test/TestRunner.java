package com.klinik.test;

import java.lang.reflect.Method;

/**
 * Runner mandiri untuk menjalankan pengujian alur pelayanan tanpa ketergantungan plugin eksternal.
 * Menjalankan seluruh skenario pengujian dan mencetak hasilnya secara komprehensif.
 */
public class TestRunner {
    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("  PENGUJIAN ALUR PELAYANAN SAMPAI PEMBAYARAN - KLINIK PRATAMA");
        System.out.println("======================================================================\n");

        AlurPelayananSampaiPembayaranTest testInstance = new AlurPelayananSampaiPembayaranTest();
        int passed = 0;
        int failed = 0;

        Method[] methods = AlurPelayananSampaiPembayaranTest.class.getDeclaredMethods();
        for (Method m : methods) {
            if (m.isAnnotationPresent(org.junit.jupiter.api.Test.class)) {
                String testName = m.getName();
                org.junit.jupiter.api.DisplayName dn = m.getAnnotation(org.junit.jupiter.api.DisplayName.class);
                if (dn != null) {
                    testName = dn.value();
                }

                try {
                    testInstance.setUp();
                    m.invoke(testInstance);
                    System.out.printf("[PASSED] %s%n", testName);
                    passed++;
                } catch (Throwable t) {
                    Throwable cause = t.getCause() != null ? t.getCause() : t;
                    System.out.printf("[FAILED] %s -> %s%n", testName, cause.getMessage());
                    failed++;
                }
            }
        }

        System.out.println("\n----------------------------------------------------------------------");
        System.out.printf("Total Uji: %d | Berhasil: %d | Gagal: %d%n", (passed + failed), passed, failed);
        System.out.println("======================================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}


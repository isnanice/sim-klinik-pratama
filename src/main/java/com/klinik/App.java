package com.klinik;

import com.klinik.controller.DokterController;
import com.klinik.controller.LayananController;
import com.klinik.controller.PasienController;
import com.klinik.dao.*;
import com.klinik.model.Role;
import com.klinik.view.MainFrame;

import javax.swing.*;

/**
 * Titik masuk utama aplikasi Sistem Informasi Manajemen Klinik Pratama.
 */
public class App {
    public static void main(String[] args) {
        // Set Look and Feel sistem
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            // Inisialisasi DAO dengan persistensi file otomatis
            PasienDAO pasienDAO = new PasienDAO();
            DokterDAO dokterDAO = new DokterDAO();
            LayananDAO layananDAO = new LayananDAO();
            PemeriksaanDAO pemeriksaanDAO = new PemeriksaanDAO(pasienDAO, dokterDAO, layananDAO);

            // Inisialisasi Controller
            DokterController dokterController = new DokterController(dokterDAO, pemeriksaanDAO, Role.ADMIN);
            LayananController layananController = new LayananController(layananDAO, Role.ADMIN);
            PasienController pasienController = new PasienController(pasienDAO, pemeriksaanDAO);

            // Tampilkan MainFrame
            MainFrame mainFrame = new MainFrame(dokterController, layananController, pasienController);
            mainFrame.setVisible(true);
        });
    }
}


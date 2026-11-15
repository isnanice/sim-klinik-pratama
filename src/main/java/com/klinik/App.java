package com.klinik;

import com.klinik.controller.LoginController;
import com.klinik.dao.DokterDAO;
import com.klinik.dao.LayananDAO;
import com.klinik.dao.PasienDAO;
import com.klinik.dao.PemeriksaanDAO;
import com.klinik.dao.PetugasDAO;
import com.klinik.dao.SuratDAO;
import com.klinik.exception.DatabaseException;
import com.klinik.exception.KlinikException;
import com.klinik.view.LoginView;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Titik masuk utama aplikasi Sistem Informasi Manajemen Klinik Pratama.
 * Menampilkan LoginView terlebih dahulu sebelum mengarahkan pengguna ke MainFrame.
 */
public class App {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        // Galat database yang lolos dari event handler ditampilkan sebagai dialog, bukan membuat aplikasi macet.
        Thread.setDefaultUncaughtExceptionHandler((thread, galat) -> {
            Throwable t = galat;
            while (t.getCause() != null && !(t instanceof KlinikException)) {
                t = t.getCause();
            }
            final String pesan = t.getMessage() != null ? t.getMessage() : t.toString();
            final boolean database = t instanceof DatabaseException;
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, pesan,
                    database ? "Kesalahan Database" : "Terjadi Kesalahan",
                    JOptionPane.ERROR_MESSAGE));
            galat.printStackTrace();
        });

        SwingUtilities.invokeLater(() -> {
            // DAO memakai PostgreSQL lewat JDBC (konfigurasi di db.properties)
            PetugasDAO petugasDAO = new PetugasDAO();
            PasienDAO pasienDAO = new PasienDAO();
            DokterDAO dokterDAO = new DokterDAO();
            LayananDAO layananDAO = new LayananDAO();
            PemeriksaanDAO pemeriksaanDAO = new PemeriksaanDAO(pasienDAO, dokterDAO, layananDAO);
            pasienDAO.setPemeriksaanDAO(pemeriksaanDAO);
            SuratDAO suratDAO = new SuratDAO();

            // Inisialisasi LoginController dan tampilkan LoginView terlebih dahulu
            LoginController loginController = new LoginController(
                    petugasDAO, pasienDAO, dokterDAO, layananDAO, pemeriksaanDAO, suratDAO
            );
            LoginView loginView = new LoginView(loginController);
            loginView.setVisible(true);
        });
    }
}

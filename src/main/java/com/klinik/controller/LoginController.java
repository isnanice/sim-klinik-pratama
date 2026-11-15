package com.klinik.controller;

import com.klinik.dao.DokterDAO;
import com.klinik.dao.LayananDAO;
import com.klinik.dao.PasienDAO;
import com.klinik.dao.PemeriksaanDAO;
import com.klinik.dao.PetugasDAO;
import com.klinik.dao.SuratDAO;
import com.klinik.exception.DataNotFoundException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.model.Petugas;
import com.klinik.util.PasswordUtil;
import com.klinik.util.SesiPengguna;
import com.klinik.validation.Validator;
import com.klinik.view.LoginView;
import com.klinik.view.MainFrame;

import javax.swing.JFrame;

/**
 * Controller untuk mengelola proses autentikasi pengguna (Login & Logout).
 * Memvalidasi input LoginView, memverifikasi kredensial lewat PetugasDAO & PasswordUtil,
 * menyimpan sesi ke SesiPengguna, serta mengarahkan navigasi ke MainFrame.
 */
public class LoginController {
    private final PetugasDAO petugasDAO;
    private PasienDAO pasienDAO;
    private DokterDAO dokterDAO;
    private LayananDAO layananDAO;
    private PemeriksaanDAO pemeriksaanDAO;
    private SuratDAO suratDAO;

    public LoginController(PetugasDAO petugasDAO) {
        this.petugasDAO = petugasDAO != null ? petugasDAO : new PetugasDAO();
    }

    public LoginController(PetugasDAO petugasDAO, PasienDAO pasienDAO, DokterDAO dokterDAO,
                           LayananDAO layananDAO, PemeriksaanDAO pemeriksaanDAO, SuratDAO suratDAO) {
        this.petugasDAO = petugasDAO != null ? petugasDAO : new PetugasDAO();
        this.pasienDAO = pasienDAO;
        this.dokterDAO = dokterDAO;
        this.layananDAO = layananDAO;
        this.pemeriksaanDAO = pemeriksaanDAO;
        this.suratDAO = suratDAO;
    }

    /**
     * Memvalidasi kredensial login (BR-01), memeriksa status_aktif akun, dan menyimpan sesi ke SesiPengguna.
     */
    public Petugas login(String username, String password)
            throws DataTidakValidException, DataNotFoundException {
        Validator.validateNotEmpty(username, "Username", "Username wajib diisi.");
        Validator.validateNotEmpty(password, "Password", "Password wajib diisi.");

        Petugas petugas = petugasDAO.cariByUsername(username.trim())
                .orElseThrow(() -> new DataNotFoundException("Username atau password salah"));

        petugasDAO.validasiStatusAktif(petugas);

        if (!PasswordUtil.verifyPassword(password, petugas.getPassword())) {
            throw new DataTidakValidException("Password", "Username atau password salah");
        }

        SesiPengguna.setSession(petugas);
        return petugas;
    }

    public Petugas autentikasi(String username, String password)
            throws DataTidakValidException, DataNotFoundException {
        return login(username, password);
    }

    /**
     * Melakukan proses login dari LoginView dan membuka MainFrame apabila autentikasi berhasil.
     */
    public MainFrame loginDanBukaMainFrame(String username, String password, JFrame loginFrame)
            throws DataTidakValidException, DataNotFoundException {
        Petugas petugas = login(username, password);

        if (pasienDAO == null) pasienDAO = new PasienDAO();
        if (dokterDAO == null) dokterDAO = new DokterDAO();
        if (layananDAO == null) layananDAO = new LayananDAO();
        if (pemeriksaanDAO == null) pemeriksaanDAO = new PemeriksaanDAO(pasienDAO, dokterDAO, layananDAO);
        pasienDAO.setPemeriksaanDAO(pemeriksaanDAO);
        if (suratDAO == null) suratDAO = new SuratDAO();

        DokterController dokterController = new DokterController(dokterDAO, pemeriksaanDAO, petugas.getRole());
        LayananController layananController = new LayananController(layananDAO, petugas.getRole());
        PasienController pasienController = new PasienController(pasienDAO, pemeriksaanDAO);
        SuratController suratController = new SuratController(suratDAO, pemeriksaanDAO);

        MainFrame mainFrame = new MainFrame(
                dokterController,
                layananController,
                pasienController,
                suratController,
                pemeriksaanDAO,
                this
        );
        mainFrame.setVisible(true);

        if (loginFrame != null) {
            loginFrame.dispose();
        }
        return mainFrame;
    }

    /**
     * Menghapus sesi aktif dan kembali menampilkan LoginView.
     */
    public void logout(JFrame currentMainFrame) {
        SesiPengguna.clearSession();
        if (currentMainFrame != null) {
            currentMainFrame.dispose();
        }
        LoginView loginView = new LoginView(this);
        loginView.setVisible(true);
    }

    public PetugasDAO getPetugasDAO() {
        return petugasDAO;
    }
}

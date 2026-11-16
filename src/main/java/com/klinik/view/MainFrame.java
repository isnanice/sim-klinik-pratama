package com.klinik.view;

import com.klinik.controller.DokterController;
import com.klinik.controller.LayananController;
import com.klinik.controller.LoginController;
import com.klinik.controller.PasienController;
import com.klinik.controller.SuratController;
import com.klinik.dao.DokterDAO;
import com.klinik.dao.LayananDAO;
import com.klinik.dao.PasienDAO;
import com.klinik.dao.PemeriksaanDAO;
import com.klinik.dao.PetugasDAO;
import com.klinik.dao.SuratDAO;
import com.klinik.model.Petugas;
import com.klinik.model.Role;
import com.klinik.util.HakAkses;
import com.klinik.util.SesiPengguna;
import com.klinik.view.theme.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Jendela Utama Aplikasi (MainFrame).
 * Menampilkan halaman sambutan (nama & role dari SesiPengguna), tombol Logout,
 * serta mengatur visibilitas menu navigasi menggunakan HakAkses.
 */
public class MainFrame extends JFrame {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainContainer = new JPanel(cardLayout);

    private final DokterController dokterController;
    private final LayananController layananController;
    private final PasienController pasienController;
    private final SuratController suratController;
    private final PemeriksaanDAO pemeriksaanDAO;
    private final LoginController loginController;

    private DokterView dokterView;
    private LayananView layananView;
    private PasienView pasienView;
    private AntreanView antreanView;
    private SuratView suratView;
    private JPanel welcomePanel;

    private JLabel lblStatusUser;
    private Role currentRole;
    private String currentUsername;

    public MainFrame(DokterController dokterController, LayananController layananController, PasienController pasienController) {
        this(
                dokterController,
                layananController,
                pasienController,
                new SuratController(new SuratDAO(), new PemeriksaanDAO(new PasienDAO(), new DokterDAO(), new LayananDAO())),
                new PemeriksaanDAO(new PasienDAO(), new DokterDAO(), new LayananDAO()),
                new LoginController(new PetugasDAO())
        );
    }

    public MainFrame(DokterController dokterController,
                     LayananController layananController,
                     PasienController pasienController,
                     SuratController suratController,
                     PemeriksaanDAO pemeriksaanDAO,
                     LoginController loginController) {
        this.dokterController = dokterController;
        this.layananController = layananController;
        this.pasienController = pasienController;
        this.suratController = suratController;
        this.pemeriksaanDAO = pemeriksaanDAO;
        this.loginController = loginController != null ? loginController : new LoginController(new PetugasDAO());

        syncFromSession();
        initUI();
    }

    private void syncFromSession() {
        Petugas sessionUser = SesiPengguna.getSession();
        if (sessionUser != null) {
            this.currentRole = sessionUser.getRole() != null ? sessionUser.getRole() : Role.PETUGAS;
            this.currentUsername = sessionUser.getNama() != null && !sessionUser.getNama().isEmpty()
                    ? sessionUser.getNama()
                    : sessionUser.getUsername();
        } else {
            this.currentRole = Role.ADMIN;
            this.currentUsername = "Siti Rahma";
        }
        if (dokterController != null) {
            dokterController.setCurrentRole(currentRole);
        }
        if (layananController != null) {
            layananController.setCurrentRole(currentRole);
        }
    }

    private void initUI() {
        setTitle("Sistem Informasi Manajemen Klinik Pratama");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 720);
        setMinimumSize(new Dimension(1024, 640));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BACKGROUND);

        // Setup Content Panels
        dokterView = new DokterView(dokterController);
        layananView = new LayananView(layananController);
        pasienView = new PasienView(pasienController);
        antreanView = new AntreanView(pemeriksaanDAO);
        suratView = new SuratView(suratController);
        welcomePanel = createWelcomePanel();

        dokterView.setRole(currentRole);
        layananView.setRole(currentRole);
        pasienView.applyRolePermissions();

        mainContainer.add(welcomePanel, "BERANDA");
        mainContainer.add(pasienView, "PASIEN");
        mainContainer.add(dokterView, "DOKTER");
        mainContainer.add(layananView, "LAYANAN");
        mainContainer.add(antreanView, "ANTREAN");
        mainContainer.add(suratView, "SURAT");

        // Setup Menu Bar dengan visibilitas HakAkses
        setJMenuBar(createAppMenuBar());

        add(mainContainer, BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);
    }

    private JMenuBar createAppMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(UITheme.PRIMARY_DARK);
        menuBar.setPreferredSize(new Dimension(menuBar.getWidth(), 36));
        menuBar.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

        // 1. Beranda
        JMenu menuBeranda = createStyledMenu("Beranda");
        JMenuItem miHome = new JMenuItem("Halaman Sambutan");
        miHome.addActionListener(e -> tampilkan("BERANDA"));
        menuBeranda.add(miHome);
        menuBeranda.setVisible(HakAkses.memilikiAkses(currentRole, HakAkses.MENU_BERANDA));

        // 2. Data Master
        JMenu menuMaster = createStyledMenu("Data Master");

        JMenuItem miPasien = new JMenuItem("Pasien");
        miPasien.setVisible(HakAkses.memilikiAkses(currentRole, HakAkses.MENU_PASIEN));
        miPasien.addActionListener(e -> tampilkan("PASIEN"));

        JMenuItem miDokter = new JMenuItem("Dokter");
        miDokter.setVisible(HakAkses.memilikiAkses(currentRole, HakAkses.MENU_DOKTER));
        miDokter.addActionListener(e -> tampilkan("DOKTER"));

        JMenuItem miLayanan = new JMenuItem("Layanan");
        miLayanan.setVisible(HakAkses.memilikiAkses(currentRole, HakAkses.MENU_LAYANAN));
        miLayanan.addActionListener(e -> tampilkan("LAYANAN"));

        menuMaster.add(miPasien);
        menuMaster.add(miDokter);
        menuMaster.add(miLayanan);
        menuMaster.setVisible(
                miPasien.isVisible() || miDokter.isVisible() || miLayanan.isVisible()
        );

        // 3. Pelayanan & Surat
        JMenu menuTransaksi = createStyledMenu("Pelayanan & Surat");

        JMenuItem miAntrean = new JMenuItem("Antrean Pemeriksaan");
        miAntrean.setVisible(HakAkses.memilikiAkses(currentRole, HakAkses.MENU_ANTREAN));
        miAntrean.addActionListener(e -> tampilkan("ANTREAN"));

        JMenuItem miSurat = new JMenuItem("Surat Izin & Rujukan");
        miSurat.setVisible(HakAkses.memilikiAkses(currentRole, HakAkses.MENU_SURAT));
        miSurat.addActionListener(e -> tampilkan("SURAT"));

        JMenuItem miRekamMedis = new JMenuItem("Rekam Medis");
        miRekamMedis.setVisible(HakAkses.memilikiAkses(currentRole, HakAkses.MENU_REKAM_MEDIS));
        miRekamMedis.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Rekam Medis dikelola oleh RekamMedisController & terintegrasi dalam alur pelayanan.",
                "Info Rekam Medis", JOptionPane.INFORMATION_MESSAGE));

        JMenuItem miPembayaran = new JMenuItem("Pembayaran");
        miPembayaran.setVisible(HakAkses.memilikiAkses(currentRole, HakAkses.MENU_PEMBAYARAN));
        miPembayaran.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Pembayaran dikelola oleh PembayaranController dengan polimorfisme tarif Layanan.",
                "Info Pembayaran", JOptionPane.INFORMATION_MESSAGE));

        menuTransaksi.add(miAntrean);
        menuTransaksi.add(miSurat);
        menuTransaksi.addSeparator();
        menuTransaksi.add(miRekamMedis);
        menuTransaksi.add(miPembayaran);
        menuTransaksi.setVisible(
                miAntrean.isVisible() || miSurat.isVisible() || miRekamMedis.isVisible() || miPembayaran.isVisible()
        );

        // 4. Akun & Sesi
        JMenu menuAkun = createStyledMenu("Akun");
        JMenuItem miLogout = new JMenuItem("Logout (Ganti Akun)");
        miLogout.addActionListener(e -> prosesLogout());

        JMenuItem miKeluar = new JMenuItem("Keluar Aplikasi");
        miKeluar.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Yakin ingin keluar dari aplikasi?",
                    "Konfirmasi Keluar", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                System.exit(0);
            }
        });

        menuAkun.add(miLogout);
        menuAkun.addSeparator();
        menuAkun.add(miKeluar);

        menuBar.add(menuBeranda);
        menuBar.add(menuMaster);
        menuBar.add(menuTransaksi);
        menuBar.add(menuAkun);

        return menuBar;
    }

    private JMenu createStyledMenu(String title) {
        JMenu menu = new JMenu(title);
        menu.setFont(UITheme.FONT_BOLD);
        menu.setForeground(Color.WHITE);
        menu.setBackground(UITheme.PRIMARY_DARK);
        menu.setOpaque(true);
        menu.setBorder(new EmptyBorder(4, 10, 4, 10));
        return menu;
    }

    private JPanel createWelcomePanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(UITheme.BACKGROUND);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(UITheme.SURFACE);
        card.setBorder(new CompoundBorder(new LineBorder(UITheme.BORDER, 1), new EmptyBorder(28, 36, 28, 36)));

        JLabel lblHeading = new JLabel("Selamat datang, " + currentUsername);
        lblHeading.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblHeading.setForeground(UITheme.PRIMARY);
        lblHeading.setAlignmentX(Component.LEFT_ALIGNMENT);

        Petugas sessionUser = SesiPengguna.getSession();
        String roleText = "Role: " + currentRole.getLabel();
        if (sessionUser != null && sessionUser.getIdDokter() != null && !sessionUser.getIdDokter().isEmpty()) {
            roleText += " | ID Dokter: " + sessionUser.getIdDokter();
        }
        JLabel lblRoleBadge = new JLabel(roleText);
        lblRoleBadge.setFont(UITheme.FONT_BOLD);
        lblRoleBadge.setForeground(UITheme.PRIMARY);
        lblRoleBadge.setBorder(new CompoundBorder(new LineBorder(UITheme.PRIMARY, 1), new EmptyBorder(4, 10, 4, 10)));
        lblRoleBadge.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblDesc = new JLabel("<html>"
                + "Gunakan menu navigasi di atas sesuai hak akses peran Anda:<br>"
                + "• <b>Data Master</b>: Kelola Pasien, Dokter, dan Layanan Medis.<br>"
                + "• <b>Pelayanan & Surat</b>: Kelola Antrean Pemeriksaan (Panggil / Batalkan) serta Surat Izin & Rujukan.<br>"
                + "• <b>Akun -> Logout</b>: Mengakhiri sesi pengguna saat ini dan kembali ke layar Login."
                + "</html>");
        lblDesc.setFont(UITheme.FONT_REGULAR);
        lblDesc.setForeground(UITheme.TEXT_MUTED);
        lblDesc.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel quickBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        quickBtnPanel.setOpaque(false);
        quickBtnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (HakAkses.memilikiAkses(currentRole, HakAkses.MENU_PASIEN)) {
            JButton btnGoPasien = UITheme.createPrimaryButton("Data Pasien");
            btnGoPasien.addActionListener(e -> tampilkan("PASIEN"));
            quickBtnPanel.add(btnGoPasien);
        }
        if (HakAkses.memilikiAkses(currentRole, HakAkses.MENU_ANTREAN)) {
            JButton btnGoAntrean = UITheme.createSecondaryButton("Antrean Pemeriksaan");
            btnGoAntrean.addActionListener(e -> tampilkan("ANTREAN"));
            quickBtnPanel.add(btnGoAntrean);
        }
        if (HakAkses.memilikiAkses(currentRole, HakAkses.MENU_SURAT)) {
            JButton btnGoSurat = UITheme.createSecondaryButton("Surat Izin & Rujukan");
            btnGoSurat.addActionListener(e -> tampilkan("SURAT"));
            quickBtnPanel.add(btnGoSurat);
        }

        JButton btnLogout = UITheme.createDangerButton("Logout");
        btnLogout.addActionListener(e -> prosesLogout());
        quickBtnPanel.add(btnLogout);

        card.add(lblHeading);
        card.add(Box.createVerticalStrut(12));
        card.add(lblRoleBadge);
        card.add(Box.createVerticalStrut(14));
        card.add(lblDesc);
        card.add(Box.createVerticalStrut(18));
        card.add(quickBtnPanel);

        wrapper.add(card);
        return wrapper;
    }

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.decode("#E2E8F0"));
        bar.setPreferredSize(new Dimension(getWidth(), 30));
        bar.setBorder(new EmptyBorder(4, 16, 4, 16));

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        lblStatusUser = new JLabel("Pengguna: " + currentUsername + " | Role: " + currentRole.getLabel() + " | " + dateStr);
        lblStatusUser.setFont(UITheme.FONT_MUTED);
        lblStatusUser.setForeground(UITheme.TEXT);

        JButton btnStatusLogout = UITheme.createSecondaryButton("Logout");
        btnStatusLogout.setPreferredSize(new Dimension(85, 22));
        btnStatusLogout.addActionListener(e -> prosesLogout());

        bar.add(lblStatusUser, BorderLayout.WEST);
        bar.add(btnStatusLogout, BorderLayout.EAST);
        return bar;
    }

    public void tampilkan(String namaKartu) {
        if ("PASIEN".equals(namaKartu)) {
            pasienView.applyRolePermissions();
            pasienView.refreshTable();
        } else if ("DOKTER".equals(namaKartu)) {
            dokterView.setRole(currentRole);
        } else if ("LAYANAN".equals(namaKartu)) {
            layananView.setRole(currentRole);
        } else if ("ANTREAN".equals(namaKartu)) {
            antreanView.refreshTable();
        } else if ("SURAT".equals(namaKartu)) {
            suratView.refreshViewForCurrentSession();
        }
        cardLayout.show(mainContainer, namaKartu);
    }

    private void prosesLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Yakin ingin logout dari sesi " + currentUsername + "?",
                "Konfirmasi Logout", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            loginController.logout(this);
        }
    }
}

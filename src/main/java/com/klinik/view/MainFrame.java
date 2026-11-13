package com.klinik.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
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

import com.klinik.controller.DokterController;
import com.klinik.controller.LayananController;
import com.klinik.controller.PasienController;
import com.klinik.model.Role;
import com.klinik.view.theme.UITheme;

/**
 * Jendela Utama Aplikasi (DESIGN.md C2).
 * Ukuran 1200x720, minimum 1024x640, resizable.
 * JMenuBar di atas, konten dengan CardLayout, dan Status Bar di bawah.
 */
public class MainFrame extends JFrame {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainContainer = new JPanel(cardLayout);

    private final DokterController dokterController;
    private final LayananController layananController;
    private final PasienController pasienController;

    private DokterView dokterView;
    private LayananView layananView;
    private PasienView pasienView;
    private JPanel welcomePanel;

    private JLabel lblStatusUser;
    private Role currentRole = Role.ADMIN;
    private String currentUsername = "Siti Rahma";

    public MainFrame(DokterController dokterController, LayananController layananController, PasienController pasienController) {
        this.dokterController = dokterController;
        this.layananController = layananController;
        this.pasienController = pasienController;

        initUI();
    }

    private void initUI() {
        setTitle("Sistem Informasi Manajemen Klinik Pratama");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 720);
        setMinimumSize(new Dimension(1024, 640));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BACKGROUND);

        // Setup Menu Bar
        setJMenuBar(createAppMenuBar());

        // Setup Content Panels
        dokterView = new DokterView(dokterController);
        layananView = new LayananView(layananController);
        pasienView = new PasienView(pasienController);
        welcomePanel = createWelcomePanel();

        mainContainer.add(welcomePanel, "BERANDA");
        mainContainer.add(dokterView, "DOKTER");
        mainContainer.add(layananView, "LAYANAN");
        mainContainer.add(pasienView, "PASIEN");

        add(mainContainer, BorderLayout.CENTER);

        // Setup Status Bar
        add(createStatusBar(), BorderLayout.SOUTH);
    }

    private JMenuBar createAppMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(UITheme.PRIMARY_DARK);
        menuBar.setPreferredSize(new Dimension(menuBar.getWidth(), 36));
        menuBar.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

        // Beranda
        JMenu menuBeranda = createStyledMenu("Beranda");
        JMenuItem miHome = new JMenuItem("Halaman Sambutan");
        miHome.addActionListener(e -> cardLayout.show(mainContainer, "BERANDA"));
        menuBeranda.add(miHome);

        // Data Master
        JMenu menuMaster = createStyledMenu("Data Master");
        JMenuItem miDokter = new JMenuItem("Dokter");
        miDokter.addActionListener(e -> {
            dokterView.setRole(currentRole);
            cardLayout.show(mainContainer, "DOKTER");
        });

        JMenuItem miLayanan = new JMenuItem("Layanan");
        miLayanan.addActionListener(e -> {
            layananView.setRole(currentRole);
            cardLayout.show(mainContainer, "LAYANAN");
        });

        JMenuItem miPasien = new JMenuItem("Pasien");
        miPasien.addActionListener(e -> cardLayout.show(mainContainer, "PASIEN"));

        menuMaster.add(miPasien);
        menuMaster.add(miDokter);
        menuMaster.add(miLayanan);

        // Transaksi
        JMenu menuTransaksi = createStyledMenu("Transaksi");
        JMenuItem miPemeriksaan = new JMenuItem("Pemeriksaan");
        miPemeriksaan.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Transaksi Pemeriksaan terintegrasi di controller & test alur pelayanan.",
                "Info", JOptionPane.INFORMATION_MESSAGE));
        JMenuItem miRekamMedis = new JMenuItem("Rekam Medis");
        miRekamMedis.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Rekam Medis dikelola oleh RekamMedisController & tervalidasi di test suite.",
                "Info", JOptionPane.INFORMATION_MESSAGE));
        JMenuItem miPembayaran = new JMenuItem("Pembayaran");
        miPembayaran.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Pembayaran dikelola oleh PembayaranController dengan polimorfisme tarif Layanan.",
                "Info", JOptionPane.INFORMATION_MESSAGE));

        menuTransaksi.add(miPemeriksaan);
        menuTransaksi.add(miRekamMedis);
        menuTransaksi.add(miPembayaran);

        // Akun & Role Switcher
        JMenu menuAkun = createStyledMenu("Akun");
        JMenuItem miSwitchRole = new JMenuItem("Ganti Role (ADMIN <-> PETUGAS)");
        miSwitchRole.addActionListener(e -> toggleRole());

        JMenuItem miKeluar = new JMenuItem("Keluar");
        miKeluar.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Yakin ingin keluar dari aplikasi?",
                    "Konfirmasi Keluar", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                System.exit(0);
            }
        });

        menuAkun.add(miSwitchRole);
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
        card.setBorder(new CompoundBorder(new LineBorder(UITheme.BORDER, 1), new EmptyBorder(24, 32, 24, 32)));
        card.setPreferredSize(new Dimension(650, 220));

        JLabel lblHeading = new JLabel("Selamat datang, " + currentUsername);
        lblHeading.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblHeading.setForeground(UITheme.PRIMARY);

        JLabel lblRoleBadge = new JLabel("Role: " + currentRole.getLabel());
        lblRoleBadge.setFont(UITheme.FONT_BOLD);
        lblRoleBadge.setForeground(UITheme.PRIMARY);
        lblRoleBadge.setBorder(new CompoundBorder(new LineBorder(UITheme.PRIMARY, 1), new EmptyBorder(3, 8, 3, 8)));

        JLabel lblDesc = new JLabel("<html>Gunakan menu <b>Data Master</b> untuk mengelola dokter dan layanan.<br>" +
                "Gunakan menu <b>Akun</b> untuk berpindah peran antara ADMIN dan PETUGAS.<br>" +
                "Perhatikan bahwa PETUGAS hanya memiliki izin baca (read-only) pada data Dokter & Layanan.</html>");
        lblDesc.setFont(UITheme.FONT_REGULAR);
        lblDesc.setForeground(UITheme.TEXT_MUTED);

        card.add(lblHeading);
        card.add(Box.createVerticalStrut(12));
        card.add(lblRoleBadge);
        card.add(Box.createVerticalStrut(14));
        card.add(lblDesc);

        wrapper.add(card);
        return wrapper;
    }

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.decode("#E2E8F0"));
        bar.setPreferredSize(new Dimension(getWidth(), 26));
        bar.setBorder(new EmptyBorder(4, 16, 4, 16));

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        lblStatusUser = new JLabel("Pengguna: " + currentUsername + " | Role: " + currentRole.getLabel() + " | " + dateStr);
        lblStatusUser.setFont(UITheme.FONT_MUTED);
        lblStatusUser.setForeground(UITheme.TEXT);

        bar.add(lblStatusUser, BorderLayout.WEST);
        return bar;
    }

    private void toggleRole() {
        if (currentRole == Role.ADMIN) {
            currentRole = Role.PETUGAS;
            currentUsername = "Rudi Hartono";
        } else {
            currentRole = Role.ADMIN;
            currentUsername = "Siti Rahma";
        }

        dokterView.setRole(currentRole);
        layananView.setRole(currentRole);

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        lblStatusUser.setText("Pengguna: " + currentUsername + " | Role: " + currentRole.getLabel() + " | " + dateStr);

        JOptionPane.showMessageDialog(this,
                "Peran berhasil dialihkan ke: " + currentRole.getLabel() + " (" + currentUsername + ")",
                "Ganti Peran", JOptionPane.INFORMATION_MESSAGE);

        // Refresh welcome panel
        mainContainer.remove(welcomePanel);
        welcomePanel = createWelcomePanel();
        mainContainer.add(welcomePanel, "BERANDA");
        cardLayout.show(mainContainer, "BERANDA");
    }
}

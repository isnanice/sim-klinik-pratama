package com.klinik.view;

import com.klinik.controller.LoginController;
import com.klinik.exception.DataNotFoundException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.view.theme.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

/**
 * Tampilan Login Aplikasi Sistem Informasi Manajemen Klinik Pratama.
 * Menggunakan JTextField, JPasswordField, dan tombol Login sesuai konstanta UITheme,
 * serta dilengkapi JScrollPane agar tidak terpotong pada berbagai resolusi/DPI layar.
 */
public class LoginView extends JFrame {
    private final LoginController controller;

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnReset;

    public LoginView(LoginController controller) {
        this.controller = controller;
        initComponents();
        setupEvents();
    }

    private void initComponents() {
        setTitle("Login - Sistem Informasi Manajemen Klinik Pratama");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 580);
        setMinimumSize(new Dimension(440, 480));
        getContentPane().setBackground(UITheme.BACKGROUND);
        setLayout(new BorderLayout());

        // Header Banner
        JPanel banner = new JPanel();
        banner.setBackground(UITheme.PRIMARY_DARK);
        banner.setBorder(new EmptyBorder(18, 24, 18, 24));
        banner.setLayout(new BoxLayout(banner, BoxLayout.Y_AXIS));

        JLabel lblAppTitle = new JLabel("SIM KLINIK PRATAMA");
        lblAppTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblAppTitle.setForeground(Color.WHITE);
        lblAppTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblAppSub = new JLabel("Silakan masuk menggunakan akun Admin, Petugas, atau Dokter");
        lblAppSub.setFont(UITheme.FONT_MUTED);
        lblAppSub.setForeground(Color.decode("#CCFBF1"));
        lblAppSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        banner.add(lblAppTitle);
        banner.add(Box.createVerticalStrut(4));
        banner.add(lblAppSub);
        add(banner, BorderLayout.NORTH);

        // Card Form Login di Tengah (Tanpa tinggi tetap yang memotong konten)
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(UITheme.BACKGROUND);
        centerWrapper.setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(UITheme.SURFACE);
        card.setBorder(new CompoundBorder(new LineBorder(UITheme.BORDER, 1), new EmptyBorder(24, 28, 24, 28)));

        JLabel lblFormTitle = new JLabel("Autentikasi Pengguna", SwingConstants.LEFT);
        lblFormTitle.setFont(UITheme.FONT_SUBTITLE);
        lblFormTitle.setForeground(UITheme.PRIMARY);
        lblFormTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lblFormTitle);
        card.add(Box.createVerticalStrut(16));

        JLabel lblUsername = new JLabel("Username *");
        lblUsername.setFont(UITheme.FONT_BOLD);
        lblUsername.setForeground(UITheme.TEXT);
        lblUsername.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lblUsername);
        card.add(Box.createVerticalStrut(6));

        txtUsername = UITheme.createTextField();
        txtUsername.setColumns(26);
        txtUsername.setPreferredSize(new Dimension(320, 34));
        txtUsername.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        txtUsername.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(txtUsername);
        card.add(Box.createVerticalStrut(14));

        JLabel lblPassword = new JLabel("Password *");
        lblPassword.setFont(UITheme.FONT_BOLD);
        lblPassword.setForeground(UITheme.TEXT);
        lblPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lblPassword);
        card.add(Box.createVerticalStrut(6));

        txtPassword = new JPasswordField(26);
        txtPassword.setFont(UITheme.FONT_REGULAR);
        txtPassword.setForeground(UITheme.TEXT);
        txtPassword.setBackground(UITheme.SURFACE);
        txtPassword.setBorder(UITheme.createFieldBorder());
        txtPassword.setPreferredSize(new Dimension(320, 34));
        txtPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        txtPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(txtPassword);
        card.add(Box.createVerticalStrut(20));

        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.X_AXIS));
        btnPanel.setOpaque(false);
        btnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnLogin = UITheme.createPrimaryButton("Login");
        btnReset = UITheme.createSecondaryButton("Bersihkan");

        btnPanel.add(btnLogin);
        btnPanel.add(Box.createHorizontalStrut(10));
        btnPanel.add(btnReset);
        card.add(btnPanel);
        card.add(Box.createVerticalStrut(18));

        // Kotak Informasi Akun Demo Lengkap
        JPanel infoBox = new JPanel();
        infoBox.setLayout(new BoxLayout(infoBox, BoxLayout.Y_AXIS));
        infoBox.setBackground(UITheme.ROW_ALT);
        infoBox.setBorder(new CompoundBorder(new LineBorder(UITheme.BORDER, 1), new EmptyBorder(10, 12, 10, 12)));
        infoBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblHintTitle = new JLabel("Daftar Akun Login Tersedia:");
        lblHintTitle.setFont(UITheme.FONT_BOLD);
        lblHintTitle.setForeground(UITheme.TEXT);
        lblHintTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblHintBody = new JLabel("<html>"
                + "• <b>Admin</b> : username <code>admin</code> | password <code>admin123</code><br>"
                + "• <b>Petugas</b> : username <code>petugas01</code> | password <code>petugas123</code><br>"
                + "• <b>Dokter 1 (DKT-001)</b> : username <code>dokter01</code> | password <code>dokter123</code><br>"
                + "• <b>Dokter 2 (DKT-002)</b> : username <code>dokter02</code> | password <code>dokter123</code>"
                + "</html>");
        lblHintBody.setFont(UITheme.FONT_MUTED);
        lblHintBody.setForeground(UITheme.TEXT_MUTED);
        lblHintBody.setAlignmentX(Component.LEFT_ALIGNMENT);

        infoBox.add(lblHintTitle);
        infoBox.add(Box.createVerticalStrut(6));
        infoBox.add(lblHintBody);
        card.add(infoBox);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        centerWrapper.add(card, gbc);

        JScrollPane scrollPane = new JScrollPane(centerWrapper);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(UITheme.BACKGROUND);

        add(scrollPane, BorderLayout.CENTER);
        setLocationRelativeTo(null);
    }

    private void setupEvents() {
        btnLogin.addActionListener(e -> prosesLogin());
        btnReset.addActionListener(e -> {
            txtUsername.setText("");
            txtPassword.setText("");
            txtUsername.requestFocusInWindow();
        });
        txtUsername.addActionListener(e -> txtPassword.requestFocusInWindow());
        txtPassword.addActionListener(e -> prosesLogin());
    }

    private void prosesLogin() {
        String username = txtUsername.getText();
        String password = new String(txtPassword.getPassword());
        try {
            controller.loginDanBukaMainFrame(username, password, this);
        } catch (DataTidakValidException | DataNotFoundException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Login Gagal", JOptionPane.WARNING_MESSAGE);
            txtPassword.setText("");
            txtPassword.requestFocusInWindow();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Terjadi kesalahan saat login: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

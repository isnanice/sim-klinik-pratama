package com.klinik.view;

import com.klinik.controller.LoginController;
import com.klinik.exception.DataNotFoundException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.view.theme.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
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
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

/**
 * Tampilan Login Aplikasi Sistem Informasi Manajemen Klinik Pratama.
 * Menggunakan JTextField, JPasswordField dengan ikon mata untuk melihat/menyembunyikan password,
 * label pesan kesalahan (BR-01), tombol Login, Bersihkan, dan Keluar.
 */
public class LoginView extends JFrame {
    private final LoginController controller;

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnTogglePassword;
    private boolean isPasswordVisible = false;
    private char defaultEchoChar;

    private JLabel lblPesanError;
    private JButton btnLogin;
    private JButton btnReset;
    private JButton btnKeluar;

    public LoginView(LoginController controller) {
        this.controller = controller;
        initComponents();
        pasangListener();
    }

    private void initComponents() {
        setTitle("Login - Sistem Informasi Manajemen Klinik Pratama");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 480);
        setMinimumSize(new Dimension(420, 420));
        getContentPane().setBackground(UITheme.BACKGROUND);
        setLayout(new BorderLayout());

        // Header Banner
        JPanel banner = new JPanel();
        banner.setBackground(UITheme.PRIMARY_DARK);
        banner.setBorder(new EmptyBorder(20, 24, 20, 24));
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
        banner.add(Box.createVerticalStrut(6));
        banner.add(lblAppSub);
        add(banner, BorderLayout.NORTH);

        // Card Form Login di Tengah
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(UITheme.BACKGROUND);
        centerWrapper.setBorder(new EmptyBorder(24, 24, 24, 24));

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

        // Field Username
        JLabel lblUsername = new JLabel("Username *");
        lblUsername.setFont(UITheme.FONT_BOLD);
        lblUsername.setForeground(UITheme.TEXT);
        lblUsername.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lblUsername);
        card.add(Box.createVerticalStrut(6));

        txtUsername = UITheme.createTextField();
        txtUsername.setColumns(24);
        txtUsername.setPreferredSize(new Dimension(320, 34));
        txtUsername.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        txtUsername.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(txtUsername);
        card.add(Box.createVerticalStrut(14));

        // Field Password dengan Ikon Mata (Show/Hide Password)
        JLabel lblPassword = new JLabel("Password *");
        lblPassword.setFont(UITheme.FONT_BOLD);
        lblPassword.setForeground(UITheme.TEXT);
        lblPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lblPassword);
        card.add(Box.createVerticalStrut(6));

        JPanel panelPassword = new JPanel(new BorderLayout());
        panelPassword.setBackground(UITheme.SURFACE);
        panelPassword.setBorder(new LineBorder(UITheme.BORDER, 1));
        panelPassword.setPreferredSize(new Dimension(320, 34));
        panelPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        panelPassword.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtPassword = new JPasswordField(24);
        txtPassword.setFont(UITheme.FONT_REGULAR);
        txtPassword.setForeground(UITheme.TEXT);
        txtPassword.setBackground(UITheme.SURFACE);
        txtPassword.setBorder(new EmptyBorder(5, 8, 5, 4));
        defaultEchoChar = txtPassword.getEchoChar();

        btnTogglePassword = new JButton(new EyeIcon(false));
        btnTogglePassword.setToolTipText("Lihat Password");
        btnTogglePassword.setFocusable(false);
        btnTogglePassword.setContentAreaFilled(false);
        btnTogglePassword.setBorder(new EmptyBorder(0, 4, 0, 8));
        btnTogglePassword.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        panelPassword.add(txtPassword, BorderLayout.CENTER);
        panelPassword.add(btnTogglePassword, BorderLayout.EAST);
        card.add(panelPassword);
        card.add(Box.createVerticalStrut(10));

        // Label Pesan Kesalahan Login (BR-01)
        lblPesanError = new JLabel(" ");
        lblPesanError.setFont(UITheme.FONT_BOLD);
        lblPesanError.setForeground(UITheme.DANGER);
        lblPesanError.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lblPesanError);
        card.add(Box.createVerticalStrut(12));

        // Tombol Login, Bersihkan, Keluar
        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.X_AXIS));
        btnPanel.setOpaque(false);
        btnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnLogin = UITheme.createPrimaryButton("Login");
        btnReset = UITheme.createSecondaryButton("Bersihkan");
        btnKeluar = UITheme.createDangerButton("Keluar");

        btnPanel.add(btnLogin);
        btnPanel.add(Box.createHorizontalStrut(8));
        btnPanel.add(btnReset);
        btnPanel.add(Box.createHorizontalStrut(8));
        btnPanel.add(btnKeluar);
        card.add(btnPanel);

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
        getRootPane().setDefaultButton(btnLogin);
        setLocationRelativeTo(null);
    }

    private void pasangListener() {
        btnLogin.addActionListener(e -> aksiMasuk());
        btnReset.addActionListener(e -> {
            txtUsername.setText("");
            txtPassword.setText("");
            if (isPasswordVisible) {
                isPasswordVisible = false;
                txtPassword.setEchoChar(defaultEchoChar != 0 ? defaultEchoChar : '\u2022');
                btnTogglePassword.setIcon(new EyeIcon(false));
                btnTogglePassword.setToolTipText("Lihat Password");
            }
            lblPesanError.setText(" ");
            txtUsername.requestFocusInWindow();
        });
        btnKeluar.addActionListener(e -> aksiKeluar());

        btnTogglePassword.addActionListener(e -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                txtPassword.setEchoChar((char) 0);
                btnTogglePassword.setIcon(new EyeIcon(true));
                btnTogglePassword.setToolTipText("Sembunyikan Password");
            } else {
                txtPassword.setEchoChar(defaultEchoChar != 0 ? defaultEchoChar : '\u2022');
                btnTogglePassword.setIcon(new EyeIcon(false));
                btnTogglePassword.setToolTipText("Lihat Password");
            }
            txtPassword.requestFocusInWindow();
        });

        txtUsername.addActionListener(e -> txtPassword.requestFocusInWindow());
        txtPassword.addActionListener(e -> aksiMasuk());
    }

    private void aksiMasuk() {
        String username = txtUsername.getText();
        String password = new String(txtPassword.getPassword());
        try {
            lblPesanError.setText(" ");
            controller.login(username, password);
            controller.bukaMainFrame(this);
        } catch (DataTidakValidException | DataNotFoundException ex) {
            lblPesanError.setText(ex.getMessage());
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Login Gagal", JOptionPane.WARNING_MESSAGE);
            txtPassword.setText("");
            txtPassword.requestFocusInWindow();
        } catch (Exception ex) {
            lblPesanError.setText(ex.getMessage());
            JOptionPane.showMessageDialog(this, "Terjadi kesalahan saat login: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void aksiKeluar() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Yakin ingin keluar dari aplikasi?",
                "Konfirmasi Keluar", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    /**
     * Ikon mata berbasis vektor Graphics2D untuk tombol lihat/sembunyikan password.
     * slash = false : Ikon mata terbuka (Lihat Password)
     * slash = true  : Ikon mata dicoret (Sembunyikan Password)
     */
    private static class EyeIcon implements Icon {
        private final boolean slash;
        private final int width = 20;
        private final int height = 18;

        public EyeIcon(boolean slash) {
            this.slash = slash;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            Color strokeColor = (c != null && c.isEnabled()) ? UITheme.TEXT_MUTED : Color.LIGHT_GRAY;
            g2.setColor(strokeColor);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int ex = x + 2;
            int ey = y + 4;
            int ew = 16;
            int eh = 10;

            // Kontur mata (lengkung atas dan bawah)
            Path2D eyePath = new Path2D.Float();
            eyePath.moveTo(ex, ey + eh / 2.0f);
            eyePath.quadTo(ex + ew / 2.0f, ey - 2.0f, ex + ew, ey + eh / 2.0f);
            eyePath.quadTo(ex + ew / 2.0f, ey + eh + 2.0f, ex, ey + eh / 2.0f);
            eyePath.closePath();
            g2.draw(eyePath);

            // Pupil mata di tengah
            int pr = 4;
            int px = ex + (ew - pr) / 2;
            int py = ey + (eh - pr) / 2;
            g2.fillOval(px, py, pr, pr);

            // Garis coret diagonal saat password terlihat (slash = true)
            if (slash) {
                g2.setColor(UITheme.DANGER);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(ex + 1, ey + eh + 2, ex + ew - 1, ey - 2);
            }

            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return width;
        }

        @Override
        public int getIconHeight() {
            return height;
        }
    }
}

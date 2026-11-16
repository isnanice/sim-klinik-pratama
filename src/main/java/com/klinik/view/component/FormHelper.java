package com.klinik.view.component;

import com.klinik.view.theme.UITheme;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.time.format.DateTimeFormatter;

/**
 * Utilitas kecil pembangun form (PRD bagian 2: VS Code tidak punya perancang GUI, jadi layout ditulis
 * dengan kode). Grid spasi 8 px, label 13 px tebal, bintang merah untuk isian wajib, sesuai DESIGN.md.
 */
public final class FormHelper {
    public static final DateTimeFormatter TANGGAL = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private FormHelper() { }

    /** Label isian. Bila wajib, ditambah bintang merah. */
    public static JLabel label(String teks, boolean wajib) {
        JLabel l = new JLabel(wajib
                ? "<html>" + teks + " <font color='#DC2626'>*</font></html>"
                : teks);
        l.setFont(UITheme.FONT_BOLD);
        l.setForeground(UITheme.TEXT);
        return l;
    }

    public static JLabel muted(String teks) {
        JLabel l = new JLabel(teks);
        l.setFont(UITheme.FONT_MUTED);
        l.setForeground(UITheme.TEXT_MUTED);
        return l;
    }

    /** Menambah satu baris "label : komponen" ke panel ber-GridBagLayout pada kolom awal {@code kolom}. */
    public static void baris(JPanel panel, int kolom, int baris, JLabel label, JComponent komponen) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = kolom;
        g.gridy = baris;
        g.anchor = GridBagConstraints.NORTHWEST;
        g.insets = new Insets(4, kolom == 0 ? 0 : 24, 4, 12);
        g.weightx = 0;
        g.ipady = 6;
        panel.add(label, g);

        GridBagConstraints k = new GridBagConstraints();
        k.gridx = kolom + 1;
        k.gridy = baris;
        k.fill = GridBagConstraints.HORIZONTAL;
        k.anchor = GridBagConstraints.NORTHWEST;
        k.insets = new Insets(4, 0, 4, 0);
        k.weightx = 1.0;
        panel.add(komponen, k);
    }

    public static JPanel kartu() {
        JPanel p = new JPanel();
        p.setBackground(UITheme.SURFACE);
        p.setBorder(UITheme.createPanelBorder());
        return p;
    }

    public static JPanel kartuTanpaPadding() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UITheme.SURFACE);
        p.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        return p;
    }

    public static JTextArea area(int baris) {
        JTextArea a = new JTextArea(baris, 20);
        a.setFont(UITheme.FONT_REGULAR);
        a.setForeground(UITheme.TEXT);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setBorder(new EmptyBorder(5, 8, 5, 8));
        return a;
    }

    public static JScrollPane gulir(JTextArea a) {
        JScrollPane sp = new JScrollPane(a);
        sp.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        return sp;
    }

    /** Mengunci atau membuka isian teks; yang terkunci berlatar #F1F5F9 (read-only di DESIGN.md). */
    public static void aktifkan(JTextField f, boolean aktif) {
        f.setEditable(aktif);
        f.setBackground(aktif ? UITheme.SURFACE : UITheme.READ_ONLY_BG);
    }

    public static void aktifkan(JTextArea a, boolean aktif) {
        a.setEditable(aktif);
        a.setBackground(aktif ? UITheme.SURFACE : UITheme.READ_ONLY_BG);
    }

    /** Header "judul kiri, keterangan kanan" di atas halaman. */
    public static JPanel header(String judul, String subjudul, String kanan) {
        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(false);
        JPanel kiri = new JPanel();
        kiri.setOpaque(false);
        kiri.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
        JLabel t = new JLabel(judul);
        t.setFont(UITheme.FONT_TITLE);
        t.setForeground(UITheme.PRIMARY);
        kiri.add(t);
        kiri.add(muted(subjudul));
        h.add(kiri, BorderLayout.WEST);
        if (kanan != null) {
            JLabel k = muted(kanan);
            h.add(k, BorderLayout.EAST);
        }
        return h;
    }

    /** Kotak informasi hanya-baca berlatar abu muda. */
    public static JLabel kotakInfo(String teks, Color warnaTeks) {
        JLabel l = new JLabel(teks);
        l.setFont(UITheme.FONT_BOLD);
        l.setForeground(warnaTeks != null ? warnaTeks : UITheme.TEXT);
        l.setOpaque(true);
        l.setBackground(UITheme.READ_ONLY_BG);
        l.setBorder(UITheme.createFieldBorder());
        l.setPreferredSize(new Dimension(l.getPreferredSize().width, 30));
        return l;
    }

    /** Pintasan keyboard di seluruh jendela, misalnya Alt+S untuk Simpan dan F5 untuk segarkan. */
    public static void pintasan(JComponent komponen, String nama, KeyStroke tombol, Runnable aksi) {
        komponen.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(tombol, nama);
        komponen.getActionMap().put(nama, new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                aksi.run();
            }
        });
    }

    public static KeyStroke alt(int kode) {
        return KeyStroke.getKeyStroke(kode, KeyEvent.ALT_DOWN_MASK);
    }

    public static KeyStroke f5() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0);
    }
}

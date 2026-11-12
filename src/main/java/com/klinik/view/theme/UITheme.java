package com.klinik.view.theme;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Token desain dan helper komponen sesuai DESIGN.md:
 * primary: #0F766E, primary-dark: #115E59, background: #F8FAFC, surface: #FFFFFF,
 * border: #CBD5E1, text: #0F172A, muted: #64748B, success: #16A34A, warning: #D97706,
 * info: #0284C7, danger: #DC2626, row-alt: #F1F5F9, row-selected: #CCFBF1.
 */
public class UITheme {
    public static final Color PRIMARY = Color.decode("#0F766E");
    public static final Color PRIMARY_DARK = Color.decode("#115E59");
    public static final Color BACKGROUND = Color.decode("#F8FAFC");
    public static final Color SURFACE = Color.decode("#FFFFFF");
    public static final Color BORDER = Color.decode("#CBD5E1");
    public static final Color TEXT = Color.decode("#0F172A");
    public static final Color TEXT_MUTED = Color.decode("#64748B");
    public static final Color SUCCESS = Color.decode("#16A34A");
    public static final Color WARNING = Color.decode("#D97706");
    public static final Color INFO = Color.decode("#0284C7");
    public static final Color DANGER = Color.decode("#DC2626");
    public static final Color ROW_ALT = Color.decode("#F1F5F9");
    public static final Color ROW_SELECTED = Color.decode("#CCFBF1");
    public static final Color READ_ONLY_BG = Color.decode("#F1F5F9");
    public static final Color DISABLED_BG = Color.decode("#E2E8F0");
    public static final Color DISABLED_TEXT = Color.decode("#94A3B8");

    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_MUTED = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 13);

    public static Border createFieldBorder() {
        return new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(5, 8, 5, 8));
    }

    public static Border createPanelBorder() {
        return new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(16, 16, 16, 16));
    }

    public static JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setBackground(PRIMARY);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new CompoundBorder(new LineBorder(PRIMARY_DARK, 1), new EmptyBorder(6, 14, 6, 14)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setBackground(SURFACE);
        btn.setForeground(PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorder(new CompoundBorder(new LineBorder(PRIMARY, 1), new EmptyBorder(6, 14, 6, 14)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static JButton createDangerButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setBackground(DANGER);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new CompoundBorder(new LineBorder(DANGER.darker(), 1), new EmptyBorder(6, 14, 6, 14)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static JTextField createTextField() {
        JTextField field = new JTextField();
        field.setFont(FONT_REGULAR);
        field.setForeground(TEXT);
        field.setBackground(SURFACE);
        field.setBorder(createFieldBorder());
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 30));
        return field;
    }

    public static JTextField createReadOnlyField() {
        JTextField field = createTextField();
        field.setEditable(false);
        field.setBackground(READ_ONLY_BG);
        return field;
    }

    public static String formatRupiah(double val) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("id", "ID"));
        return "Rp " + nf.format(Math.round(val));
    }
}


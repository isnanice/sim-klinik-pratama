package com.klinik.view.component;

import com.klinik.view.theme.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * Utilitas helper untuk menata tabel Swing agar konsisten dengan DESIGN.md:
 * - Header teal (#0F766E) dengan teks putih tebal 13px
 * - Tinggi baris 28px
 * - Baris bergantian #F1F5F9
 * - Baris terpilih #CCFBF1
 * - Teks status tebal berwarna (Aktif: #16A34A, Nonaktif: #DC2626)
 */
public class TableStyleHelper {

    public static void applyStyle(JTable table) {
        table.setRowHeight(28);
        table.setFont(UITheme.FONT_REGULAR);
        table.setForeground(UITheme.TEXT);
        table.setGridColor(UITheme.BORDER);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setSelectionBackground(UITheme.ROW_SELECTED);
        table.setSelectionForeground(UITheme.TEXT);

        JTableHeader header = table.getTableHeader();
        header.setFont(UITheme.FONT_BOLD);
        header.setBackground(UITheme.PRIMARY);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(header.getWidth(), 32));
        header.setReorderingAllowed(false);

        // Renderer khusus baris selang-seling & badge status
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? UITheme.SURFACE : UITheme.ROW_ALT);
                    c.setForeground(UITheme.TEXT);
                } else {
                    c.setBackground(UITheme.ROW_SELECTED);
                    c.setForeground(UITheme.TEXT);
                }

                String text = value != null ? value.toString() : "";
                if ("Aktif".equalsIgnoreCase(text) || "SELESAI".equalsIgnoreCase(text) || "LUNAS".equalsIgnoreCase(text)) {
                    c.setForeground(UITheme.SUCCESS);
                    setFont(UITheme.FONT_BOLD);
                } else if ("Nonaktif".equalsIgnoreCase(text) || "DIBATALKAN".equalsIgnoreCase(text)) {
                    c.setForeground(UITheme.DANGER);
                    setFont(UITheme.FONT_BOLD);
                } else if ("MENUNGGU".equalsIgnoreCase(text) || "BELUM_LUNAS".equalsIgnoreCase(text) || text.startsWith("MENUNGGU_")) {
                    c.setForeground(UITheme.WARNING);
                    setFont(UITheme.FONT_BOLD);
                } else if ("TERDAFTAR".equalsIgnoreCase(text)) {
                    c.setForeground(UITheme.INFO);
                    setFont(UITheme.FONT_BOLD);
                } else if (text.startsWith("Rp ")) {
                    setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }
                return c;
            }
        });
    }
}


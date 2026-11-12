package com.klinik.view;

import com.klinik.model.DapatDicetak;
import com.klinik.view.theme.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Dialog modal bersama untuk menampilkan rincian cetak dari objek DapatDicetak (DESIGN.md C9).
 */
public class DetailDialog extends JDialog {

    public DetailDialog(Frame parent, String title, DapatDicetak entity) {
        super(parent, title, true);
        initUI(entity);
    }

    public DetailDialog(Dialog parent, String title, DapatDicetak entity) {
        super(parent, title, true);
        initUI(entity);
    }

    private void initUI(DapatDicetak entity) {
        setSize(460, 400);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.BACKGROUND);

        JPanel contentPanel = new JPanel(new BorderLayout(8, 8));
        contentPanel.setBackground(UITheme.BACKGROUND);
        contentPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JTextArea textArea = new JTextArea();
        textArea.setFont(UITheme.FONT_MONO);
        textArea.setEditable(false);
        textArea.setBackground(UITheme.SURFACE);
        textArea.setForeground(UITheme.TEXT);
        textArea.setBorder(new EmptyBorder(12, 12, 12, 12));
        textArea.setText(entity != null ? entity.cetakRingkasan() : "Tidak ada data rincian.");

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(UITheme.BACKGROUND);
        JButton btnTutup = UITheme.createPrimaryButton("Tutup");
        btnTutup.addActionListener(e -> dispose());
        buttonPanel.add(btnTutup);

        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(contentPanel, BorderLayout.CENTER);
    }

    public static void show(Component parent, String title, DapatDicetak entity) {
        Window window = SwingUtilities.getWindowAncestor(parent);
        if (window instanceof Frame) {
            new DetailDialog((Frame) window, title, entity).setVisible(true);
        } else if (window instanceof Dialog) {
            new DetailDialog((Dialog) window, title, entity).setVisible(true);
        } else {
            new DetailDialog((Frame) null, title, entity).setVisible(true);
        }
    }
}


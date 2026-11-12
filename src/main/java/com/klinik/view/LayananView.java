package com.klinik.view;

import com.klinik.controller.LayananController;
import com.klinik.exception.AksesDitolakException;
import com.klinik.exception.DataNotFoundException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.model.Layanan;
import com.klinik.model.LayananSpesialis;
import com.klinik.model.LayananUmum;
import com.klinik.model.Role;
import com.klinik.view.component.TableStyleHelper;
import com.klinik.view.theme.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Tampilan Kelola Data Layanan (DESIGN.md C5).
 * - Field: ID (read-only), Nama Layanan*, Jenis Layanan* (Umum / Spesialis),
 *   Tarif Dasar* (angka >= 0), Keterangan (text area), Status Aktif.
 * - Tombol: Tambah, Ubah, Nonaktifkan, Bersihkan. (TIDAK ADA TOMBOL HAPUS).
 * - Tabel: ID, Nama Layanan, Jenis, Tarif Dasar, Status.
 * - Catatan info: "Tarif akhir layanan Spesialis ditambah biaya konsultasi spesialis."
 */
public class LayananView extends JPanel {
    private final LayananController controller;

    // Form
    private JTextField txtId;
    private JTextField txtNamaLayanan;
    private JComboBox<String> cmbJenis;
    private JTextField txtTarifDasar;
    private JTextArea txtKeterangan;
    private JCheckBox chkAktif;
    private JLabel lblRoleNote;

    // Buttons
    private JButton btnTambah;
    private JButton btnUbah;
    private JButton btnNonaktifkan;
    private JButton btnDetail;
    private JButton btnBersihkan;

    // Search & Table
    private JTextField txtCari;
    private JButton btnCari;
    private JButton btnReset;
    private JTable tblLayanan;
    private DefaultTableModel tableModel;

    public LayananView(LayananController controller) {
        this.controller = controller;
        initComponents();
        setupEvents();
        refreshTable();
        applyRolePermissions();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        // Header Layar
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel lblTitle = new JLabel("Data Layanan");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.PRIMARY);
        headerPanel.add(lblTitle, BorderLayout.WEST);

        lblRoleNote = new JLabel("Anda login sebagai PETUGAS. Data layanan hanya dapat dilihat.");
        lblRoleNote.setFont(UITheme.FONT_MUTED);
        lblRoleNote.setForeground(UITheme.TEXT_MUTED);
        lblRoleNote.setVisible(controller.getCurrentRole() == Role.PETUGAS);
        headerPanel.add(lblRoleNote, BorderLayout.EAST);

        // NORTH Container (Header + Form + Action Row)
        JPanel northContainer = new JPanel();
        northContainer.setLayout(new BoxLayout(northContainer, BoxLayout.Y_AXIS));
        northContainer.setOpaque(false);
        northContainer.add(headerPanel);
        northContainer.add(Box.createVerticalStrut(12));

        // Form Card
        JPanel formCard = new JPanel(new GridLayout(1, 2, 24, 0));
        formCard.setBackground(UITheme.SURFACE);
        formCard.setBorder(UITheme.createPanelBorder());

        // Kolom Kiri
        JPanel leftCol = new JPanel();
        leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));
        leftCol.setOpaque(false);

        txtId = UITheme.createReadOnlyField();
        txtNamaLayanan = UITheme.createTextField();
        cmbJenis = new JComboBox<>(new String[]{"Umum", "Spesialis"});
        cmbJenis.setFont(UITheme.FONT_REGULAR);
        cmbJenis.setPreferredSize(new Dimension(cmbJenis.getPreferredSize().width, 30));

        txtTarifDasar = UITheme.createTextField();
        txtTarifDasar.setHorizontalAlignment(SwingConstants.RIGHT);
        txtTarifDasar.setText("0");

        JLabel lblNoteTarif = new JLabel("Tarif akhir layanan Spesialis ditambah biaya konsultasi spesialis.");
        lblNoteTarif.setFont(UITheme.FONT_MUTED);
        lblNoteTarif.setForeground(UITheme.TEXT_MUTED);

        addField(leftCol, "ID Layanan", txtId);
        addField(leftCol, "Nama Layanan *", txtNamaLayanan);
        addField(leftCol, "Jenis Layanan *", cmbJenis);
        addField(leftCol, "Tarif Dasar *", txtTarifDasar);
        leftCol.add(lblNoteTarif);
        leftCol.add(Box.createVerticalStrut(8));

        // Kolom Kanan
        JPanel rightCol = new JPanel();
        rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));
        rightCol.setOpaque(false);

        txtKeterangan = new JTextArea(3, 20);
        txtKeterangan.setFont(UITheme.FONT_REGULAR);
        txtKeterangan.setBorder(UITheme.createFieldBorder());
        txtKeterangan.setLineWrap(true);
        txtKeterangan.setWrapStyleWord(true);
        JScrollPane spKet = new JScrollPane(txtKeterangan);
        spKet.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));

        chkAktif = new JCheckBox("Aktif");
        chkAktif.setFont(UITheme.FONT_REGULAR);
        chkAktif.setBackground(UITheme.SURFACE);
        chkAktif.setSelected(true);

        addField(rightCol, "Keterangan", spKet);
        addField(rightCol, "Status", chkAktif);

        formCard.add(leftCol);
        formCard.add(rightCol);
        northContainer.add(formCard);
        northContainer.add(Box.createVerticalStrut(12));

        // TOOLBAR & SEARCH ROW
        JPanel actionRow = new JPanel(new BorderLayout(8, 8));
        actionRow.setOpaque(false);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnPanel.setOpaque(false);
        btnTambah = UITheme.createPrimaryButton("Tambah");
        btnUbah = UITheme.createPrimaryButton("Ubah");
        btnNonaktifkan = UITheme.createSecondaryButton("Nonaktifkan");
        btnDetail = UITheme.createSecondaryButton("Detail");
        btnBersihkan = UITheme.createSecondaryButton("Bersihkan");

        btnPanel.add(btnTambah);
        btnPanel.add(btnUbah);
        btnPanel.add(btnNonaktifkan);
        btnPanel.add(btnDetail);
        btnPanel.add(btnBersihkan);
        actionRow.add(btnPanel, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchPanel.setOpaque(false);
        JLabel lblCari = new JLabel("Cari Layanan:");
        lblCari.setFont(UITheme.FONT_REGULAR);
        txtCari = UITheme.createTextField();
        txtCari.setPreferredSize(new Dimension(180, 30));
        btnCari = UITheme.createSecondaryButton("Cari");
        btnReset = UITheme.createSecondaryButton("Reset");

        searchPanel.add(lblCari);
        searchPanel.add(txtCari);
        searchPanel.add(btnCari);
        searchPanel.add(btnReset);
        actionRow.add(searchPanel, BorderLayout.EAST);

        northContainer.add(actionRow);
        add(northContainer, BorderLayout.NORTH);

        // TABLE SECTION (CENTER)
        String[] columns = {"ID", "Nama Layanan", "Jenis", "Tarif Dasar", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblLayanan = new JTable(tableModel);
        TableStyleHelper.applyStyle(tblLayanan);
        JScrollPane tableScroll = new JScrollPane(tblLayanan);
        tableScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        tableScroll.getViewport().setBackground(UITheme.SURFACE);

        add(tableScroll, BorderLayout.CENTER);
    }

    private void addField(JPanel panel, String labelText, Component comp) {
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(UITheme.FONT_REGULAR);
        lbl.setForeground(UITheme.TEXT);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lbl);
        panel.add(Box.createVerticalStrut(4));
        if (comp instanceof JComponent) {
            ((JComponent) comp).setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        panel.add(comp);
        panel.add(Box.createVerticalStrut(8));
    }

    private void setupEvents() {
        btnTambah.addActionListener(e -> tambahData());
        btnUbah.addActionListener(e -> ubahData());
        btnNonaktifkan.addActionListener(e -> toggleAktifData());
        btnDetail.addActionListener(e -> tampilkanDetail());
        btnBersihkan.addActionListener(e -> bersihkanForm());

        btnCari.addActionListener(e -> cariData());
        btnReset.addActionListener(e -> {
            txtCari.setText("");
            refreshTable();
        });

        tblLayanan.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tblLayanan.getSelectedRow() != -1) {
                isiFormDariTabel(tblLayanan.getSelectedRow());
            }
        });
    }

    public void applyRolePermissions() {
        boolean isAdmin = controller.getCurrentRole() == Role.ADMIN;
        lblRoleNote.setVisible(!isAdmin);

        txtNamaLayanan.setEnabled(isAdmin);
        txtNamaLayanan.setBackground(isAdmin ? UITheme.SURFACE : UITheme.READ_ONLY_BG);
        cmbJenis.setEnabled(isAdmin);
        txtTarifDasar.setEnabled(isAdmin);
        txtTarifDasar.setBackground(isAdmin ? UITheme.SURFACE : UITheme.READ_ONLY_BG);
        txtKeterangan.setEnabled(isAdmin);
        txtKeterangan.setBackground(isAdmin ? UITheme.SURFACE : UITheme.READ_ONLY_BG);
        chkAktif.setEnabled(isAdmin);

        btnTambah.setEnabled(isAdmin);
        btnUbah.setEnabled(isAdmin);
        btnNonaktifkan.setEnabled(isAdmin);
        btnBersihkan.setEnabled(isAdmin);

        btnDetail.setEnabled(true);
        btnCari.setEnabled(true);
        btnReset.setEnabled(true);
        txtCari.setEnabled(true);
        tblLayanan.setEnabled(true);
    }

    public void setRole(Role role) {
        controller.setCurrentRole(role);
        applyRolePermissions();
    }

    private void refreshTable() {
        populateTable(controller.getAllLayanan());
    }

    private void populateTable(List<Layanan> list) {
        tableModel.setRowCount(0);
        for (Layanan l : list) {
            tableModel.addRow(new Object[]{
                    l.getId(),
                    l.getNamaLayanan(),
                    l.getJenisLayanan(),
                    UITheme.formatRupiah(l.getTarifDasar()),
                    l.isAktif() ? "Aktif" : "Nonaktif"
            });
        }
    }

    private void isiFormDariTabel(int row) {
        String id = (String) tableModel.getValueAt(row, 0);
        controller.getAllLayanan().stream()
                .filter(l -> l.getId().equals(id))
                .findFirst()
                .ifPresent(l -> {
                    txtId.setText(l.getId());
                    txtNamaLayanan.setText(l.getNamaLayanan());
                    cmbJenis.setSelectedItem(l.getJenisLayanan());
                    txtTarifDasar.setText(String.valueOf(Math.round(l.getTarifDasar())));
                    txtKeterangan.setText(l.getKeterangan());
                    chkAktif.setSelected(l.isAktif());
                    btnNonaktifkan.setText(l.isAktif() ? "Nonaktifkan" : "Aktifkan");
                });
    }

    private void bersihkanForm() {
        txtId.setText("");
        txtNamaLayanan.setText("");
        cmbJenis.setSelectedIndex(0);
        txtTarifDasar.setText("0");
        txtKeterangan.setText("");
        chkAktif.setSelected(true);
        btnNonaktifkan.setText("Nonaktifkan");
        tblLayanan.clearSelection();
    }

    private void tambahData() {
        try {
            double tarif = Double.parseDouble(txtTarifDasar.getText().trim().replace(".", "").replace(",", "."));
            String jenis = (String) cmbJenis.getSelectedItem();

            Layanan layanan;
            if ("Spesialis".equalsIgnoreCase(jenis)) {
                layanan = new LayananSpesialis();
            } else {
                layanan = new LayananUmum();
            }

            layanan.setNamaLayanan(txtNamaLayanan.getText());
            layanan.setTarifDasar(tarif);
            layanan.setKeterangan(txtKeterangan.getText());
            layanan.setAktif(chkAktif.isSelected());

            controller.tambahLayanan(layanan);
            JOptionPane.showMessageDialog(this, "Layanan berhasil ditambahkan dengan ID: " + layanan.getId(),
                    "Sukses", JOptionPane.INFORMATION_MESSAGE);
            bersihkanForm();
            refreshTable();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Tarif dasar harus berupa angka.", "Validasi Gagal", JOptionPane.WARNING_MESSAGE);
        } catch (DataTidakValidException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Validasi Gagal", JOptionPane.WARNING_MESSAGE);
        } catch (AksesDitolakException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Akses Ditolak", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Terjadi kesalahan: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void ubahData() {
        String id = txtId.getText().trim();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Pilih data layanan dari tabel yang ingin diubah.",
                    "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            double tarif = Double.parseDouble(txtTarifDasar.getText().trim().replace(".", "").replace(",", "."));
            String jenis = (String) cmbJenis.getSelectedItem();

            Layanan layanan;
            if ("Spesialis".equalsIgnoreCase(jenis)) {
                layanan = new LayananSpesialis();
            } else {
                layanan = new LayananUmum();
            }

            layanan.setId(id);
            layanan.setNamaLayanan(txtNamaLayanan.getText());
            layanan.setTarifDasar(tarif);
            layanan.setKeterangan(txtKeterangan.getText());
            layanan.setAktif(chkAktif.isSelected());

            controller.ubahLayanan(layanan);
            JOptionPane.showMessageDialog(this, "Data layanan berhasil diperbarui.",
                    "Sukses", JOptionPane.INFORMATION_MESSAGE);
            bersihkanForm();
            refreshTable();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Tarif dasar harus berupa angka.", "Validasi Gagal", JOptionPane.WARNING_MESSAGE);
        } catch (DataTidakValidException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Validasi Gagal", JOptionPane.WARNING_MESSAGE);
        } catch (AksesDitolakException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Akses Ditolak", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Terjadi kesalahan: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void toggleAktifData() {
        String id = txtId.getText().trim();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Pilih data layanan dari tabel.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Layanan l = controller.getLayananById(id);
            boolean newStatus = !l.isAktif();
            controller.nonaktifkanLayanan(id, newStatus);
            JOptionPane.showMessageDialog(this,
                    "Layanan " + l.getNamaLayanan() + " berhasil di" + (newStatus ? "aktifkan." : "nonaktifkan."),
                    "Sukses", JOptionPane.INFORMATION_MESSAGE);
            bersihkanForm();
            refreshTable();
        } catch (AksesDitolakException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Akses Ditolak", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void tampilkanDetail() {
        String id = txtId.getText().trim();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Pilih baris layanan pada tabel untuk melihat detail.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Layanan l = controller.getLayananById(id);
            DetailDialog.show(this, "Detail Layanan", l);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cariData() {
        String keyword = txtCari.getText();
        List<Layanan> hasil = controller.cariLayanan(keyword);
        populateTable(hasil);
    }
}


package com.klinik.view;

import com.klinik.controller.PemeriksaanController;
import com.klinik.exception.AksesDitolakException;
import com.klinik.exception.DatabaseException;
import com.klinik.exception.DataNotFoundException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.model.Pemeriksaan;
import com.klinik.model.StatusPemeriksaan;
import com.klinik.view.component.TableStyleHelper;
import com.klinik.view.theme.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Tampilan Antrean Pemeriksaan per Tahap.
 * Terhubung ke PemeriksaanController (satu-satunya pengubah status, PRD bagian 16) dengan aturan BR-14 & BR-15:
 * - Tombol Panggil menambah hitungan jumlahPanggilan.
 * - Tombol Batalkan meminta konfirmasi dan hanya mengizinkan pembatalan apabila jumlahPanggilan >= 3 serta alasan diisi.
 */
public class AntreanView extends JPanel {
    private static final DateTimeFormatter FORMAT_TANGGAL = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final PemeriksaanController pemeriksaanController;

    private JComboBox<String> cmbFilterStatus;
    private JTextArea txtAlasanBatal;

    private JButton btnPanggil;
    private JButton btnBatalkan;
    private JButton btnDetail;
    private JButton btnRefresh;

    private JTable tblAntrean;
    private DefaultTableModel tableModel;

    public AntreanView(PemeriksaanController pemeriksaanController) {
        this.pemeriksaanController = pemeriksaanController;
        initComponents();
        pasangListener();
        refreshTable();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        // HEADER & FILTER BAR (NORTH)
        JPanel northContainer = new JPanel();
        northContainer.setLayout(new BoxLayout(northContainer, BoxLayout.Y_AXIS));
        northContainer.setOpaque(false);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel lblTitle = new JLabel("Antrean Pemeriksaan Pasien");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.PRIMARY);
        headerPanel.add(lblTitle, BorderLayout.WEST);

        JLabel lblSubtitle = new JLabel("BR-14 & BR-15: Pembatalan antrean wajib dipanggil minimal 3 kali dan disertai alasan.");
        lblSubtitle.setFont(UITheme.FONT_MUTED);
        lblSubtitle.setForeground(UITheme.TEXT_MUTED);
        headerPanel.add(lblSubtitle, BorderLayout.EAST);

        northContainer.add(headerPanel);
        northContainer.add(Box.createVerticalStrut(12));

        // Card Kontrol Antrean
        JPanel controlCard = new JPanel(new BorderLayout(12, 12));
        controlCard.setBackground(UITheme.SURFACE);
        controlCard.setBorder(UITheme.createPanelBorder());

        // Baris Atas Kontrol: Filter Tahap (status hanya maju lewat pengisian tiap tahap, bukan dari sini)
        JPanel topRow = new JPanel(new BorderLayout(8, 8));
        topRow.setOpaque(false);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterPanel.setOpaque(false);
        JLabel lblFilter = new JLabel("Filter Tahap Antrean:");
        lblFilter.setFont(UITheme.FONT_BOLD);
        cmbFilterStatus = new JComboBox<>(new String[]{
                "Semua Tahap",
                StatusPemeriksaan.TERDAFTAR.name(),
                StatusPemeriksaan.TTV_SELESAI.name(),
                StatusPemeriksaan.DIPERIKSA.name(),
                StatusPemeriksaan.SELESAI.name(),
                StatusPemeriksaan.DIBATALKAN.name()
        });
        cmbFilterStatus.setFont(UITheme.FONT_REGULAR);
        cmbFilterStatus.setPreferredSize(new Dimension(180, 30));

        btnRefresh = UITheme.createSecondaryButton("Segarkan");
        filterPanel.add(lblFilter);
        filterPanel.add(cmbFilterStatus);
        filterPanel.add(btnRefresh);
        topRow.add(filterPanel, BorderLayout.WEST);

        // Baris Bawah Kontrol: Tombol Panggil, JTextArea Alasan Pembatalan + JScrollPane, Tombol Batalkan
        JPanel bottomRow = new JPanel(new BorderLayout(8, 8));
        bottomRow.setOpaque(false);

        JPanel callActionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        callActionPanel.setOpaque(false);
        btnPanggil = UITheme.createPrimaryButton("Panggil Antrean (+1)");
        btnDetail = UITheme.createSecondaryButton("Detail Kunjungan");
        callActionPanel.add(btnPanggil);
        callActionPanel.add(btnDetail);
        bottomRow.add(callActionPanel, BorderLayout.WEST);

        JPanel cancelPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        cancelPanel.setOpaque(false);
        JLabel lblAlasan = new JLabel("Alasan Batal (wajib):");
        lblAlasan.setFont(UITheme.FONT_REGULAR);
        txtAlasanBatal = new JTextArea(2, 20);
        txtAlasanBatal.setFont(UITheme.FONT_REGULAR);
        txtAlasanBatal.setBorder(UITheme.createFieldBorder());
        txtAlasanBatal.setLineWrap(true);
        txtAlasanBatal.setWrapStyleWord(true);
        JScrollPane spAlasan = new JScrollPane(txtAlasanBatal);
        spAlasan.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));

        btnBatalkan = UITheme.createDangerButton("Batalkan Antrean");

        cancelPanel.add(lblAlasan);
        cancelPanel.add(spAlasan);
        cancelPanel.add(btnBatalkan);
        bottomRow.add(cancelPanel, BorderLayout.EAST);

        controlCard.add(topRow, BorderLayout.NORTH);
        controlCard.add(bottomRow, BorderLayout.SOUTH);

        northContainer.add(controlCard);
        add(northContainer, BorderLayout.NORTH);

        // TABEL ANTREAN (CENTER)
        String[] columns = {
                "ID Pemeriksaan", "No. Antrean", "Nama Pasien", "Dokter Tujuan",
                "Layanan", "Tanggal", "Tahap / Status", "Jml Panggilan", "Alasan Batal"
        };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblAntrean = new JTable(tableModel);
        TableStyleHelper.applyStyle(tblAntrean);
        JScrollPane tableScroll = new JScrollPane(tblAntrean);
        tableScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        tableScroll.getViewport().setBackground(UITheme.SURFACE);

        add(tableScroll, BorderLayout.CENTER);
    }

    private void pasangListener() {
        cmbFilterStatus.addActionListener(e -> refreshTable());
        btnRefresh.addActionListener(e -> refreshTable());
        btnPanggil.addActionListener(e -> panggilPasienTerpilih());
        btnBatalkan.addActionListener(e -> batalkanPasienTerpilih());
        btnDetail.addActionListener(e -> tampilkanDetailTerpilih());

        tblAntrean.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tblAntrean.getSelectedRow() != -1) {
                int row = tblAntrean.getSelectedRow();
                String alasan = (String) tableModel.getValueAt(row, 8);
                if (alasan != null && !"-".equals(alasan)) {
                    txtAlasanBatal.setText(alasan);
                } else {
                    txtAlasanBatal.setText("");
                }
            }
        });
    }

    public void refreshTable() {
        if (pemeriksaanController == null) return;
        String selectedFilter = (String) cmbFilterStatus.getSelectedItem();
        try {
            StatusPemeriksaan st = (selectedFilter == null || "Semua Tahap".equalsIgnoreCase(selectedFilter))
                    ? null : StatusPemeriksaan.fromString(selectedFilter);
            populateTable(pemeriksaanController.kunjunganPerTahap(st));
        } catch (AksesDitolakException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Akses Ditolak", JOptionPane.ERROR_MESSAGE);
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Kesalahan Database", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void populateTable(List<Pemeriksaan> list) {
        tableModel.setRowCount(0);
        for (Pemeriksaan p : list) {
            tableModel.addRow(new Object[]{
                    p.getId(),
                    p.getNomorAntreanPoli() != null ? p.getNomorAntreanPoli() : "-",
                    p.getPasien() != null ? p.getPasien().getNama() : "-",
                    p.getDokter() != null ? p.getDokter().getNama() : "-",
                    p.getLayanan() != null ? p.getLayanan().getNamaLayanan() : "-",
                    p.getTanggalPemeriksaan() != null ? p.getTanggalPemeriksaan().format(FORMAT_TANGGAL) : "-",
                    p.getStatus() != null ? p.getStatus().getLabel() : "-",
                    p.getJumlahPanggilan(),
                    p.getAlasanBatal() != null && !p.getAlasanBatal().isEmpty() ? p.getAlasanBatal() : "-"
            });
        }
    }

    private String getSelectedPemeriksaanId() {
        int row = tblAntrean.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Pilih baris antrean pemeriksaan pada tabel terlebih dahulu.",
                    "Peringatan", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return (String) tableModel.getValueAt(row, 0);
    }

    private void panggilPasienTerpilih() {
        String id = getSelectedPemeriksaanId();
        if (id == null) return;
        try {
            Pemeriksaan p = pemeriksaanController.panggil(id);
            String namaPasien = p.getPasien() != null ? p.getPasien().getNama() : id;
            JOptionPane.showMessageDialog(this,
                    "Memanggil antrean " + p.getNomorAntreanPoli() + " - " + namaPasien
                            + "\nTotal panggilan saat ini: " + p.getJumlahPanggilan() + " kali.",
                    "Panggilan Antrean", JOptionPane.INFORMATION_MESSAGE);
            refreshTable();
        } catch (DataTidakValidException | DataNotFoundException | DatabaseException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Validasi Gagal", JOptionPane.WARNING_MESSAGE);
        } catch (AksesDitolakException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Akses Ditolak", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void batalkanPasienTerpilih() {
        String id = getSelectedPemeriksaanId();
        if (id == null) return;
        int confirm = JOptionPane.showConfirmDialog(this,
                "Yakin ingin membatalkan kunjungan " + id + "?",
                "Konfirmasi Pembatalan", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            String alasan = txtAlasanBatal.getText().trim();
            Pemeriksaan p = pemeriksaanController.batalkan(id, alasan);
            txtAlasanBatal.setText("");
            JOptionPane.showMessageDialog(this,
                    "Antrean " + p.getNomorAntreanPoli() + " (" + p.getId() + ") berhasil dibatalkan.\nAlasan: " + p.getAlasanBatal(),
                    "Pembatalan Berhasil", JOptionPane.INFORMATION_MESSAGE);
            refreshTable();
        } catch (DataTidakValidException | DataNotFoundException | DatabaseException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Validasi Gagal", JOptionPane.WARNING_MESSAGE);
        } catch (AksesDitolakException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Akses Ditolak", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void tampilkanDetailTerpilih() {
        String id = getSelectedPemeriksaanId();
        if (id == null) return;
        try {
            pemeriksaanController.cariKunjungan(id)
                    .ifPresent(p -> DetailDialog.show(this, "Detail Antrean Pemeriksaan", p));
        } catch (AksesDitolakException | DatabaseException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Gagal", JOptionPane.ERROR_MESSAGE);
        }
    }
}

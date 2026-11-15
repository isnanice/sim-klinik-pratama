package com.klinik.view;

import com.klinik.dao.PemeriksaanDAO;
import com.klinik.exception.AksesDitolakException;
import com.klinik.exception.DataNotFoundException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.model.Pemeriksaan;
import com.klinik.model.StatusPemeriksaan;
import com.klinik.util.HakAkses;
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
import java.util.stream.Collectors;

/**
 * Tampilan Antrean Pemeriksaan per Tahap.
 * Terhubung ke PemeriksaanDAO eksisting dengan menerapkan HakAkses serta aturan BR-14 & BR-15:
 * - Tombol Panggil menambah hitungan jumlahPanggilan.
 * - Tombol Batalkan meminta konfirmasi dan hanya mengizinkan pembatalan apabila jumlahPanggilan >= 3 serta alasan diisi.
 */
public class AntreanView extends JPanel {
    private static final DateTimeFormatter FORMAT_TANGGAL = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final PemeriksaanDAO pemeriksaanDAO;

    private JComboBox<String> cmbFilterStatus;
    private JComboBox<StatusPemeriksaan> cmbUbahTahap;
    private JTextArea txtAlasanBatal;

    private JButton btnPanggil;
    private JButton btnBatalkan;
    private JButton btnUbahTahap;
    private JButton btnDetail;
    private JButton btnRefresh;

    private JTable tblAntrean;
    private DefaultTableModel tableModel;

    public AntreanView(PemeriksaanDAO pemeriksaanDAO) {
        this.pemeriksaanDAO = pemeriksaanDAO;
        initComponents();
        setupEvents();
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

        // Baris Atas Kontrol: Filter Tahap & Ubah Tahap
        JPanel topRow = new JPanel(new BorderLayout(8, 8));
        topRow.setOpaque(false);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterPanel.setOpaque(false);
        JLabel lblFilter = new JLabel("Filter Tahap Antrean:");
        lblFilter.setFont(UITheme.FONT_BOLD);
        cmbFilterStatus = new JComboBox<>(new String[]{
                "Semua Tahap",
                StatusPemeriksaan.TERDAFTAR.name(),
                StatusPemeriksaan.MENUNGGU_TTV.name(),
                StatusPemeriksaan.TTV_SELESAI.name(),
                StatusPemeriksaan.MENUNGGU_DOKTER.name(),
                StatusPemeriksaan.DIPERIKSA.name(),
                StatusPemeriksaan.MENUNGGU_OBAT.name(),
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

        JPanel stagePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        stagePanel.setOpaque(false);
        JLabel lblStage = new JLabel("Ubah Tahap ke:");
        lblStage.setFont(UITheme.FONT_REGULAR);
        cmbUbahTahap = new JComboBox<>(new StatusPemeriksaan[]{
                StatusPemeriksaan.MENUNGGU_TTV,
                StatusPemeriksaan.TTV_SELESAI,
                StatusPemeriksaan.MENUNGGU_DOKTER,
                StatusPemeriksaan.DIPERIKSA,
                StatusPemeriksaan.MENUNGGU_OBAT,
                StatusPemeriksaan.SELESAI
        });
        cmbUbahTahap.setFont(UITheme.FONT_REGULAR);
        cmbUbahTahap.setPreferredSize(new Dimension(170, 30));
        btnUbahTahap = UITheme.createSecondaryButton("Perbarui Tahap");
        stagePanel.add(lblStage);
        stagePanel.add(cmbUbahTahap);
        stagePanel.add(btnUbahTahap);
        topRow.add(stagePanel, BorderLayout.EAST);

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

    private void setupEvents() {
        cmbFilterStatus.addActionListener(e -> refreshTable());
        btnRefresh.addActionListener(e -> refreshTable());
        btnPanggil.addActionListener(e -> panggilPasienTerpilih());
        btnBatalkan.addActionListener(e -> batalkanPasienTerpilih());
        btnUbahTahap.addActionListener(e -> ubahTahapTerpilih());
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
        if (pemeriksaanDAO == null) return;
        String selectedFilter = (String) cmbFilterStatus.getSelectedItem();
        List<Pemeriksaan> list = pemeriksaanDAO.findAll();
        if (selectedFilter != null && !"Semua Tahap".equalsIgnoreCase(selectedFilter)) {
            StatusPemeriksaan st = StatusPemeriksaan.fromString(selectedFilter);
            if (st != null) {
                list = list.stream()
                        .filter(p -> p.getStatus() == st)
                        .collect(Collectors.toList());
            }
        }
        populateTable(list);
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
            HakAkses.periksaAkses(HakAkses.KELOLA_ANTREAN);
            Pemeriksaan p = pemeriksaanDAO.panggilAntrean(id);
            String namaPasien = p.getPasien() != null ? p.getPasien().getNama() : id;
            JOptionPane.showMessageDialog(this,
                    "Memanggil antrean " + p.getNomorAntreanPoli() + " - " + namaPasien
                            + "\nTotal panggilan saat ini: " + p.getJumlahPanggilan() + " kali.",
                    "Panggilan Antrean", JOptionPane.INFORMATION_MESSAGE);
            refreshTable();
        } catch (DataTidakValidException | DataNotFoundException e) {
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
            HakAkses.periksaAkses(HakAkses.KELOLA_ANTREAN);
            String alasan = txtAlasanBatal.getText().trim();
            Pemeriksaan p = pemeriksaanDAO.batalkanAntrean(id, alasan);
            txtAlasanBatal.setText("");
            JOptionPane.showMessageDialog(this,
                    "Antrean " + p.getNomorAntreanPoli() + " (" + p.getId() + ") berhasil dibatalkan.\nAlasan: " + p.getAlasanBatal(),
                    "Pembatalan Berhasil", JOptionPane.INFORMATION_MESSAGE);
            refreshTable();
        } catch (DataTidakValidException | DataNotFoundException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Validasi Gagal", JOptionPane.WARNING_MESSAGE);
        } catch (AksesDitolakException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Akses Ditolak", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void ubahTahapTerpilih() {
        String id = getSelectedPemeriksaanId();
        if (id == null) return;
        try {
            HakAkses.periksaAkses(HakAkses.KELOLA_ANTREAN);
            Pemeriksaan p = pemeriksaanDAO.findById(id)
                    .orElseThrow(() -> new DataNotFoundException("Pemeriksaan " + id + " tidak ditemukan."));
            if (p.getStatus() == StatusPemeriksaan.DIBATALKAN) {
                throw new DataTidakValidException("Status", "Antrean yang sudah DIBATALKAN tidak dapat diubah tahapnya.");
            }
            StatusPemeriksaan target = (StatusPemeriksaan) cmbUbahTahap.getSelectedItem();
            if (target != null) {
                p.setStatus(target);
                pemeriksaanDAO.update(p);
                JOptionPane.showMessageDialog(this,
                        "Status tahap antrean " + p.getId() + " diperbarui menjadi: " + target.getLabel(),
                        "Sukses", JOptionPane.INFORMATION_MESSAGE);
                refreshTable();
            }
        } catch (DataTidakValidException | DataNotFoundException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Validasi Gagal", JOptionPane.WARNING_MESSAGE);
        } catch (AksesDitolakException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Akses Ditolak", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void tampilkanDetailTerpilih() {
        String id = getSelectedPemeriksaanId();
        if (id == null) return;
        pemeriksaanDAO.findById(id).ifPresent(p -> DetailDialog.show(this, "Detail Antrean Pemeriksaan", p));
    }
}

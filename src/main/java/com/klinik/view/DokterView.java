package com.klinik.view;

import com.klinik.controller.DokterController;
import com.klinik.exception.AksesDitolakException;
import com.klinik.exception.DataMasihDipakaiException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.model.Dokter;
import com.klinik.model.JenisKelamin;
import com.klinik.model.Role;
import com.klinik.view.component.TableStyleHelper;
import com.klinik.view.theme.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Tampilan Kelola Data Dokter (DESIGN.md C4).
 * Pola layout: Form di atas (NORTH), Toolbar & Pencarian, Tabel di bawah (CENTER).
 * Mendukung mode peran ADMIN (penuh) dan PETUGAS (hanya lihat).
 */
public class DokterView extends JPanel {
    private final DokterController controller;

    // Komponen Form
    private JTextField txtId;
    private JTextField txtNama;
    private JRadioButton rbLaki;
    private JRadioButton rbPerempuan;
    private ButtonGroup bgGender;
    private JTextArea txtAlamat;
    private JTextField txtNoTelepon;
    private JTextField txtNomorRegistrasi;
    private JTextField txtBidangPelayanan;
    private JCheckBox chkAktif;
    private JLabel lblRoleNote;

    // Tombol Form
    private JButton btnTambah;
    private JButton btnUbah;
    private JButton btnHapus;
    private JButton btnNonaktifkan;
    private JButton btnDetail;
    private JButton btnBersihkan;

    // Pencarian & Tabel
    private JTextField txtCari;
    private JComboBox<String> cmbKriteria;
    private JButton btnCari;
    private JButton btnReset;
    private JTable tblDokter;
    private DefaultTableModel tableModel;

    public DokterView(DokterController controller) {
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

        // HEADER LAYAR
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel lblTitle = new JLabel("Data Dokter");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.PRIMARY);
        headerPanel.add(lblTitle, BorderLayout.WEST);

        lblRoleNote = new JLabel("Anda login sebagai PETUGAS. Data dokter hanya dapat dilihat.");
        lblRoleNote.setFont(UITheme.FONT_MUTED);
        lblRoleNote.setForeground(UITheme.TEXT_MUTED);
        lblRoleNote.setVisible(controller.getCurrentRole() == Role.PETUGAS);
        headerPanel.add(lblRoleNote, BorderLayout.EAST);

        // FORM SECTION (NORTH)
        JPanel northContainer = new JPanel();
        northContainer.setLayout(new BoxLayout(northContainer, BoxLayout.Y_AXIS));
        northContainer.setOpaque(false);
        northContainer.add(headerPanel);
        northContainer.add(Box.createVerticalStrut(12));

        JPanel formCard = new JPanel(new GridLayout(1, 2, 24, 0));
        formCard.setBackground(UITheme.SURFACE);
        formCard.setBorder(UITheme.createPanelBorder());

        // Kolom Kiri Form
        JPanel leftCol = new JPanel();
        leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));
        leftCol.setOpaque(false);

        txtId = UITheme.createReadOnlyField();
        txtNama = UITheme.createTextField();

        rbLaki = new JRadioButton("Laki-laki");
        rbLaki.setFont(UITheme.FONT_REGULAR);
        rbLaki.setBackground(UITheme.SURFACE);
        rbPerempuan = new JRadioButton("Perempuan");
        rbPerempuan.setFont(UITheme.FONT_REGULAR);
        rbPerempuan.setBackground(UITheme.SURFACE);
        bgGender = new ButtonGroup();
        bgGender.add(rbLaki);
        bgGender.add(rbPerempuan);
        rbLaki.setSelected(true);

        JPanel genderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        genderPanel.setOpaque(false);
        genderPanel.add(rbLaki);
        genderPanel.add(rbPerempuan);

        txtAlamat = new JTextArea(3, 20);
        txtAlamat.setFont(UITheme.FONT_REGULAR);
        txtAlamat.setBorder(UITheme.createFieldBorder());
        txtAlamat.setLineWrap(true);
        txtAlamat.setWrapStyleWord(true);
        JScrollPane spAlamat = new JScrollPane(txtAlamat);
        spAlamat.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));

        addField(leftCol, "ID Dokter", txtId);
        addField(leftCol, "Nama *", txtNama);
        addField(leftCol, "Jenis Kelamin *", genderPanel);
        addField(leftCol, "Alamat", spAlamat);

        // Kolom Kanan Form
        JPanel rightCol = new JPanel();
        rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));
        rightCol.setOpaque(false);

        txtNoTelepon = UITheme.createTextField();
        txtNomorRegistrasi = UITheme.createTextField();
        txtBidangPelayanan = UITheme.createTextField();
        chkAktif = new JCheckBox("Aktif");
        chkAktif.setFont(UITheme.FONT_REGULAR);
        chkAktif.setBackground(UITheme.SURFACE);
        chkAktif.setSelected(true);

        addField(rightCol, "No. Telepon *", txtNoTelepon);
        addField(rightCol, "Nomor Registrasi *", txtNomorRegistrasi);
        addField(rightCol, "Bidang Pelayanan *", txtBidangPelayanan);
        addField(rightCol, "Status", chkAktif);

        formCard.add(leftCol);
        formCard.add(rightCol);
        northContainer.add(formCard);
        northContainer.add(Box.createVerticalStrut(12));

        // TOOLBAR & SEARCH ROW
        JPanel actionRow = new JPanel(new BorderLayout(8, 8));
        actionRow.setOpaque(false);

        // Tombol aksi di kiri
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnPanel.setOpaque(false);
        btnTambah = UITheme.createPrimaryButton("Tambah");
        btnUbah = UITheme.createPrimaryButton("Ubah");
        btnHapus = UITheme.createDangerButton("Hapus");
        btnNonaktifkan = UITheme.createSecondaryButton("Nonaktifkan");
        btnDetail = UITheme.createSecondaryButton("Detail");
        btnBersihkan = UITheme.createSecondaryButton("Bersihkan");

        btnPanel.add(btnTambah);
        btnPanel.add(btnUbah);
        btnPanel.add(btnHapus);
        btnPanel.add(btnNonaktifkan);
        btnPanel.add(btnDetail);
        btnPanel.add(btnBersihkan);
        actionRow.add(btnPanel, BorderLayout.WEST);

        // Pencarian di kanan
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchPanel.setOpaque(false);
        JLabel lblCari = new JLabel("Cari berdasarkan:");
        lblCari.setFont(UITheme.FONT_REGULAR);
        cmbKriteria = new JComboBox<>(new String[]{"Semua", "Nama", "Bidang Pelayanan"});
        cmbKriteria.setFont(UITheme.FONT_REGULAR);
        cmbKriteria.setPreferredSize(new Dimension(140, 30));

        txtCari = UITheme.createTextField();
        txtCari.setPreferredSize(new Dimension(160, 30));

        btnCari = UITheme.createSecondaryButton("Cari");
        btnReset = UITheme.createSecondaryButton("Reset");

        searchPanel.add(lblCari);
        searchPanel.add(cmbKriteria);
        searchPanel.add(txtCari);
        searchPanel.add(btnCari);
        searchPanel.add(btnReset);
        actionRow.add(searchPanel, BorderLayout.EAST);

        northContainer.add(actionRow);
        add(northContainer, BorderLayout.NORTH);

        // TABLE SECTION (CENTER)
        String[] columns = {"ID", "Nama", "Bidang Pelayanan", "Nomor Registrasi", "No. Telepon", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblDokter = new JTable(tableModel);
        TableStyleHelper.applyStyle(tblDokter);
        JScrollPane tableScroll = new JScrollPane(tblDokter);
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
        btnHapus.addActionListener(e -> hapusData());
        btnNonaktifkan.addActionListener(e -> toggleAktifData());
        btnDetail.addActionListener(e -> tampilkanDetail());
        btnBersihkan.addActionListener(e -> bersihkanForm());

        btnCari.addActionListener(e -> cariData());
        btnReset.addActionListener(e -> {
            txtCari.setText("");
            cmbKriteria.setSelectedIndex(0);
            refreshTable();
        });

        tblDokter.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tblDokter.getSelectedRow() != -1) {
                isiFormDariTabel(tblDokter.getSelectedRow());
            }
        });
    }

    public void applyRolePermissions() {
        boolean isAdmin = controller.getCurrentRole() == Role.ADMIN;
        lblRoleNote.setVisible(!isAdmin);

        // Mode Petugas: hanya-lihat (form disabled & tombol modifikasi disabled)
        txtNama.setEnabled(isAdmin);
        txtNama.setBackground(isAdmin ? UITheme.SURFACE : UITheme.READ_ONLY_BG);
        rbLaki.setEnabled(isAdmin);
        rbPerempuan.setEnabled(isAdmin);
        txtAlamat.setEnabled(isAdmin);
        txtAlamat.setBackground(isAdmin ? UITheme.SURFACE : UITheme.READ_ONLY_BG);
        txtNoTelepon.setEnabled(isAdmin);
        txtNoTelepon.setBackground(isAdmin ? UITheme.SURFACE : UITheme.READ_ONLY_BG);
        txtNomorRegistrasi.setEnabled(isAdmin);
        txtNomorRegistrasi.setBackground(isAdmin ? UITheme.SURFACE : UITheme.READ_ONLY_BG);
        txtBidangPelayanan.setEnabled(isAdmin);
        txtBidangPelayanan.setBackground(isAdmin ? UITheme.SURFACE : UITheme.READ_ONLY_BG);
        chkAktif.setEnabled(isAdmin);

        btnTambah.setEnabled(isAdmin);
        btnUbah.setEnabled(isAdmin);
        btnHapus.setEnabled(isAdmin);
        btnNonaktifkan.setEnabled(isAdmin);
        btnBersihkan.setEnabled(isAdmin);

        // Detail, pencarian, dan tabel tetap aktif
        btnDetail.setEnabled(true);
        btnCari.setEnabled(true);
        btnReset.setEnabled(true);
        txtCari.setEnabled(true);
        tblDokter.setEnabled(true);
    }

    public void setRole(Role role) {
        controller.setCurrentRole(role);
        applyRolePermissions();
    }

    private void refreshTable() {
        populateTable(controller.getAllDokter());
    }

    private void populateTable(List<Dokter> list) {
        tableModel.setRowCount(0);
        for (Dokter d : list) {
            tableModel.addRow(new Object[]{
                    d.getId(),
                    d.getNama(),
                    d.getBidangPelayanan(),
                    d.getNomorRegistrasi(),
                    d.getNoTelepon(),
                    d.isAktif() ? "Aktif" : "Nonaktif"
            });
        }
    }

    private void isiFormDariTabel(int row) {
        String id = (String) tableModel.getValueAt(row, 0);
        controller.getAllDokter().stream()
                .filter(d -> d.getId().equals(id))
                .findFirst()
                .ifPresent(d -> {
                    txtId.setText(d.getId());
                    txtNama.setText(d.getNama());
                    if (d.getJenisKelamin() == JenisKelamin.PEREMPUAN) {
                        rbPerempuan.setSelected(true);
                    } else {
                        rbLaki.setSelected(true);
                    }
                    txtAlamat.setText(d.getAlamat());
                    txtNoTelepon.setText(d.getNoTelepon());
                    txtNomorRegistrasi.setText(d.getNomorRegistrasi());
                    txtBidangPelayanan.setText(d.getBidangPelayanan());
                    chkAktif.setSelected(d.isAktif());
                    btnNonaktifkan.setText(d.isAktif() ? "Nonaktifkan" : "Aktifkan");
                });
    }

    private void bersihkanForm() {
        txtId.setText("");
        txtNama.setText("");
        rbLaki.setSelected(true);
        txtAlamat.setText("");
        txtNoTelepon.setText("");
        txtNomorRegistrasi.setText("");
        txtBidangPelayanan.setText("");
        chkAktif.setSelected(true);
        btnNonaktifkan.setText("Nonaktifkan");
        tblDokter.clearSelection();
    }

    private void tambahData() {
        try {
            Dokter d = new Dokter();
            d.setNama(txtNama.getText());
            d.setJenisKelamin(rbPerempuan.isSelected() ? JenisKelamin.PEREMPUAN : JenisKelamin.LAKI_LAKI);
            d.setAlamat(txtAlamat.getText());
            d.setNoTelepon(txtNoTelepon.getText());
            d.setNomorRegistrasi(txtNomorRegistrasi.getText());
            d.setBidangPelayanan(txtBidangPelayanan.getText());
            d.setAktif(chkAktif.isSelected());

            controller.tambahDokter(d);
            JOptionPane.showMessageDialog(this, "Data dokter berhasil ditambahkan dengan ID: " + d.getId(),
                    "Sukses", JOptionPane.INFORMATION_MESSAGE);
            bersihkanForm();
            refreshTable();
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
            JOptionPane.showMessageDialog(this, "Pilih data dokter dari tabel yang ingin diubah.",
                    "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Dokter d = new Dokter();
            d.setId(id);
            d.setNama(txtNama.getText());
            d.setJenisKelamin(rbPerempuan.isSelected() ? JenisKelamin.PEREMPUAN : JenisKelamin.LAKI_LAKI);
            d.setAlamat(txtAlamat.getText());
            d.setNoTelepon(txtNoTelepon.getText());
            d.setNomorRegistrasi(txtNomorRegistrasi.getText());
            d.setBidangPelayanan(txtBidangPelayanan.getText());
            d.setAktif(chkAktif.isSelected());

            controller.ubahDokter(d);
            JOptionPane.showMessageDialog(this, "Data dokter berhasil diperbarui.",
                    "Sukses", JOptionPane.INFORMATION_MESSAGE);
            bersihkanForm();
            refreshTable();
        } catch (DataTidakValidException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Validasi Gagal", JOptionPane.WARNING_MESSAGE);
        } catch (AksesDitolakException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Akses Ditolak", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Terjadi kesalahan: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void hapusData() {
        String id = txtId.getText().trim();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Pilih data dokter yang akan dihapus.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int konfirmasi = JOptionPane.showConfirmDialog(this,
                "Hapus data dokter " + id + " secara permanen?",
                "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (konfirmasi == JOptionPane.YES_OPTION) {
            try {
                controller.hapusDokter(id);
                JOptionPane.showMessageDialog(this, "Data dokter berhasil dihapus.", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                bersihkanForm();
                refreshTable();
            } catch (DataMasihDipakaiException e) {
                // Sesuai DESIGN.md C10: Data Masih Dipakai
                int opt = JOptionPane.showOptionDialog(this,
                        e.getMessage(),
                        "Data Tidak Dapat Dihapus",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        new Object[]{"Nonaktifkan", "Batal"},
                        "Nonaktifkan");

                if (opt == 0) { // Pilih Nonaktifkan
                    try {
                        controller.nonaktifkanDokter(id, false);
                        JOptionPane.showMessageDialog(this, "Dokter berhasil dinonaktifkan.", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                        bersihkanForm();
                        refreshTable();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } catch (AksesDitolakException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Akses Ditolak", JOptionPane.ERROR_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Gagal menghapus: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void toggleAktifData() {
        String id = txtId.getText().trim();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Pilih data dokter dari tabel.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Dokter d = controller.getDokterById(id);
            boolean newStatus = !d.isAktif();
            controller.nonaktifkanDokter(id, newStatus);
            JOptionPane.showMessageDialog(this,
                    "Dokter " + d.getNama() + " berhasil di" + (newStatus ? "aktifkan." : "nonaktifkan."),
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
            JOptionPane.showMessageDialog(this, "Pilih baris dokter pada tabel untuk melihat detail.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Dokter d = controller.getDokterById(id);
            DetailDialog.show(this, "Detail Dokter", d);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cariData() {
        String keyword = txtCari.getText();
        String kriteria = (String) cmbKriteria.getSelectedItem();
        List<Dokter> hasil = controller.cariDokter(keyword, kriteria);
        populateTable(hasil);
    }
}


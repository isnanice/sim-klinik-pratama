package com.klinik.view;

import com.klinik.controller.PasienController;
import com.klinik.exception.AksesDitolakException;
import com.klinik.exception.DataMasihDipakaiException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.model.JenisKelamin;
import com.klinik.model.Pasien;
import com.klinik.model.Role;
import com.klinik.util.HakAkses;
import com.klinik.util.SesiPengguna;
import com.klinik.view.component.TableStyleHelper;
import com.klinik.view.theme.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Tampilan Kelola Data Pasien.
 * Terintegrasi dengan PasienController, DetailDialog (untuk DapatDicetak),
 * TableStyleHelper, serta otorisasi HakAkses.
 */
public class PasienView extends JPanel {
    private static final LocalDate DEFAULT_TANGGAL_LAHIR = LocalDate.of(2000, 1, 1);
    private static final DateTimeFormatter FORMAT_TANGGAL = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final PasienController controller;

    // Komponen Form
    private JTextField txtId;
    private JTextField txtNik;
    private JTextField txtNama;
    private JRadioButton rbLaki;
    private JRadioButton rbPerempuan;
    private ButtonGroup bgGender;
    private JSpinner spnTanggalLahir;
    private JTextArea txtAlamat;
    private JTextField txtNoTelepon;
    private JTextField txtNoBpjs;
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
    private JTable tblPasien;
    private DefaultTableModel tableModel;

    public PasienView(PasienController controller) {
        this.controller = controller;
        initComponents();
        setupEvents();
        applyRolePermissions();
        refreshTable();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        // HEADER LAYAR
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel lblTitle = new JLabel("Data Pasien");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.PRIMARY);
        headerPanel.add(lblTitle, BorderLayout.WEST);

        lblRoleNote = new JLabel("Mode Baca: Role Anda hanya diizinkan melihat dan mencari data pasien.");
        lblRoleNote.setFont(UITheme.FONT_MUTED);
        lblRoleNote.setForeground(UITheme.TEXT_MUTED);
        lblRoleNote.setVisible(false);
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
        txtNik = UITheme.createTextField();
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

        txtAlamat = new JTextArea(2, 20);
        txtAlamat.setFont(UITheme.FONT_REGULAR);
        txtAlamat.setBorder(UITheme.createFieldBorder());
        txtAlamat.setLineWrap(true);
        txtAlamat.setWrapStyleWord(true);
        JScrollPane spAlamat = new JScrollPane(txtAlamat);
        spAlamat.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));

        addField(leftCol, "ID Pasien", txtId);
        addField(leftCol, "NIK (16 digit, opsional)", txtNik);
        addField(leftCol, "Nama *", txtNama);
        addField(leftCol, "Jenis Kelamin *", genderPanel);
        addField(leftCol, "Alamat", spAlamat);

        // Kolom Kanan Form
        JPanel rightCol = new JPanel();
        rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));
        rightCol.setOpaque(false);

        spnTanggalLahir = new JSpinner(new SpinnerDateModel(toDate(DEFAULT_TANGGAL_LAHIR), null, null, Calendar.DAY_OF_MONTH));
        spnTanggalLahir.setEditor(new JSpinner.DateEditor(spnTanggalLahir, "dd-MM-yyyy"));
        spnTanggalLahir.setFont(UITheme.FONT_REGULAR);
        spnTanggalLahir.setPreferredSize(new Dimension(spnTanggalLahir.getPreferredSize().width, 30));
        spnTanggalLahir.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        txtNoTelepon = UITheme.createTextField();
        txtNoBpjs = UITheme.createTextField();
        chkAktif = new JCheckBox("Aktif");
        chkAktif.setFont(UITheme.FONT_REGULAR);
        chkAktif.setBackground(UITheme.SURFACE);
        chkAktif.setSelected(true);

        addField(rightCol, "Tanggal Lahir * (dd-MM-yyyy)", spnTanggalLahir);
        addField(rightCol, "No. Telepon *", txtNoTelepon);
        addField(rightCol, "No. BPJS (opsional, 13 digit)", txtNoBpjs);
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

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchPanel.setOpaque(false);
        JLabel lblCari = new JLabel("Cari berdasarkan:");
        lblCari.setFont(UITheme.FONT_REGULAR);
        cmbKriteria = new JComboBox<>(new String[]{"Semua", "ID", "NIK", "Nama", "No. BPJS"});
        cmbKriteria.setFont(UITheme.FONT_REGULAR);
        cmbKriteria.setPreferredSize(new Dimension(120, 30));

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
        String[] columns = {"ID", "NIK", "Nama", "Jenis Kelamin", "Tgl Lahir", "No. Telepon", "No. BPJS", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblPasien = new JTable(tableModel);
        TableStyleHelper.applyStyle(tblPasien);
        JScrollPane tableScroll = new JScrollPane(tblPasien);
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
        txtCari.addActionListener(e -> cariData());
        btnReset.addActionListener(e -> {
            txtCari.setText("");
            cmbKriteria.setSelectedIndex(0);
            refreshTable();
        });

        tblPasien.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tblPasien.getSelectedRow() != -1) {
                isiFormDariTabel(tblPasien.getSelectedRow());
            }
        });
    }

    public void applyRolePermissions() {
        boolean canManage = HakAkses.memilikiAkses(HakAkses.KELOLA_PASIEN);
        lblRoleNote.setVisible(!canManage);

        txtNik.setEnabled(canManage);
        txtNik.setBackground(canManage ? UITheme.SURFACE : UITheme.READ_ONLY_BG);
        txtNama.setEnabled(canManage);
        txtNama.setBackground(canManage ? UITheme.SURFACE : UITheme.READ_ONLY_BG);
        rbLaki.setEnabled(canManage);
        rbPerempuan.setEnabled(canManage);
        spnTanggalLahir.setEnabled(canManage);
        txtAlamat.setEnabled(canManage);
        txtAlamat.setBackground(canManage ? UITheme.SURFACE : UITheme.READ_ONLY_BG);
        txtNoTelepon.setEnabled(canManage);
        txtNoTelepon.setBackground(canManage ? UITheme.SURFACE : UITheme.READ_ONLY_BG);
        txtNoBpjs.setEnabled(canManage);
        txtNoBpjs.setBackground(canManage ? UITheme.SURFACE : UITheme.READ_ONLY_BG);
        chkAktif.setEnabled(canManage);

        btnTambah.setEnabled(canManage);
        btnUbah.setEnabled(canManage);
        btnHapus.setEnabled(canManage);
        btnNonaktifkan.setEnabled(canManage);
        btnBersihkan.setEnabled(canManage);
    }

    public void setRole(Role role) {
        boolean canManage = HakAkses.memilikiAkses(role, HakAkses.KELOLA_PASIEN);
        lblRoleNote.setVisible(!canManage);
        btnTambah.setEnabled(canManage);
        btnUbah.setEnabled(canManage);
        btnHapus.setEnabled(canManage);
        btnNonaktifkan.setEnabled(canManage);
    }

    // ---------- Tabel ----------

    public void refreshTable() {
        populateTable(controller.getAllPasien());
    }

    private void populateTable(List<Pasien> list) {
        tableModel.setRowCount(0);
        for (Pasien p : list) {
            tableModel.addRow(new Object[]{
                    p.getId(),
                    p.getNik() != null && !p.getNik().isEmpty() ? p.getNik() : "-",
                    p.getNama(),
                    p.getJenisKelamin() != null ? p.getJenisKelamin().getLabel() : "-",
                    p.getTanggalLahir() != null ? p.getTanggalLahir().format(FORMAT_TANGGAL) : "-",
                    p.getNoTelepon(),
                    p.getNoBpjs() != null && !p.getNoBpjs().isEmpty() ? p.getNoBpjs() : "-",
                    p.isAktif() ? "Aktif" : "Nonaktif"
            });
        }
    }

    private void isiFormDariTabel(int row) {
        String id = (String) tableModel.getValueAt(row, 0);
        try {
            Pasien p = controller.getPasienById(id);
            txtId.setText(p.getId());
            txtNik.setText(p.getNik() != null ? p.getNik() : "");
            txtNama.setText(p.getNama());
            if (p.getJenisKelamin() == JenisKelamin.PEREMPUAN) {
                rbPerempuan.setSelected(true);
            } else {
                rbLaki.setSelected(true);
            }
            spnTanggalLahir.setValue(toDate(p.getTanggalLahir() != null ? p.getTanggalLahir() : DEFAULT_TANGGAL_LAHIR));
            txtAlamat.setText(p.getAlamat());
            txtNoTelepon.setText(p.getNoTelepon());
            txtNoBpjs.setText(p.getNoBpjs());
            chkAktif.setSelected(p.isAktif());
            btnNonaktifkan.setText(p.isAktif() ? "Nonaktifkan" : "Aktifkan");
        } catch (Exception e) {
            tampilkanError(e);
        }
    }

    private void bersihkanForm() {
        txtId.setText("");
        txtNik.setText("");
        txtNama.setText("");
        rbLaki.setSelected(true);
        spnTanggalLahir.setValue(toDate(DEFAULT_TANGGAL_LAHIR));
        txtAlamat.setText("");
        txtNoTelepon.setText("");
        txtNoBpjs.setText("");
        chkAktif.setSelected(true);
        btnNonaktifkan.setText("Nonaktifkan");
        tblPasien.clearSelection();
    }

    // ---------- Aksi ----------

    private Pasien bacaForm() throws DataTidakValidException {
        Pasien p = new Pasien();
        p.setNik(txtNik.getText());
        p.setNama(txtNama.getText());
        p.setJenisKelamin(rbPerempuan.isSelected() ? JenisKelamin.PEREMPUAN : JenisKelamin.LAKI_LAKI);
        p.setTanggalLahir(toLocalDate((Date) spnTanggalLahir.getValue()));
        p.setAlamat(txtAlamat.getText());
        p.setNoTelepon(txtNoTelepon.getText());
        p.setNoBpjs(txtNoBpjs.getText());
        p.setAktif(chkAktif.isSelected());
        return p;
    }

    private void tambahData() {
        try {
            Pasien p = bacaForm();
            controller.tambahPasien(p);
            JOptionPane.showMessageDialog(this, "Data pasien berhasil ditambahkan dengan ID: " + p.getId(),
                    "Sukses", JOptionPane.INFORMATION_MESSAGE);
            bersihkanForm();
            refreshTable();
        } catch (DataTidakValidException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Validasi Gagal", JOptionPane.WARNING_MESSAGE);
        } catch (AksesDitolakException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Akses Ditolak", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            tampilkanError(e);
        }
    }

    private void ubahData() {
        String id = txtId.getText().trim();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Pilih data pasien dari tabel yang ingin diubah.",
                    "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Pasien lama = controller.getPasienById(id);
            Pasien p = bacaForm();
            p.setId(id);
            p.setTanggalRegistrasi(lama.getTanggalRegistrasi());
            controller.ubahPasien(p);
            JOptionPane.showMessageDialog(this, "Data pasien berhasil diperbarui.",
                    "Sukses", JOptionPane.INFORMATION_MESSAGE);
            bersihkanForm();
            refreshTable();
        } catch (DataTidakValidException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Validasi Gagal", JOptionPane.WARNING_MESSAGE);
        } catch (AksesDitolakException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Akses Ditolak", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            tampilkanError(e);
        }
    }

    private void hapusData() {
        String id = txtId.getText().trim();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Pilih data pasien yang akan dihapus.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int konfirmasi = JOptionPane.showConfirmDialog(this,
                "Hapus data pasien " + id + " secara permanen?",
                "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (konfirmasi != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            controller.hapusPasien(id);
            JOptionPane.showMessageDialog(this, "Data pasien berhasil dihapus.", "Sukses", JOptionPane.INFORMATION_MESSAGE);
            bersihkanForm();
            refreshTable();
        } catch (DataMasihDipakaiException e) {
            int opt = JOptionPane.showOptionDialog(this,
                    e.getMessage(),
                    "Data Tidak Dapat Dihapus",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    new Object[]{"Nonaktifkan", "Batal"},
                    "Nonaktifkan");
            if (opt == 0) {
                try {
                    controller.nonaktifkanPasien(id, false);
                    JOptionPane.showMessageDialog(this, "Pasien berhasil dinonaktifkan.", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                    bersihkanForm();
                    refreshTable();
                } catch (Exception ex) {
                    tampilkanError(ex);
                }
            }
        } catch (AksesDitolakException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Akses Ditolak", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            tampilkanError(e);
        }
    }

    private void toggleAktifData() {
        String id = txtId.getText().trim();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Pilih data pasien dari tabel.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Pasien p = controller.getPasienById(id);
            boolean statusBaru = !p.isAktif();
            controller.nonaktifkanPasien(id, statusBaru);
            JOptionPane.showMessageDialog(this,
                    "Pasien " + p.getNama() + " berhasil di" + (statusBaru ? "aktifkan." : "nonaktifkan."),
                    "Sukses", JOptionPane.INFORMATION_MESSAGE);
            bersihkanForm();
            refreshTable();
        } catch (AksesDitolakException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Akses Ditolak", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            tampilkanError(e);
        }
    }

    private void tampilkanDetail() {
        String id = txtId.getText().trim();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Pilih baris pasien pada tabel untuk melihat detail.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            DetailDialog.show(this, "Detail Pasien", controller.getPasienById(id));
        } catch (Exception e) {
            tampilkanError(e);
        }
    }

    private void cariData() {
        populateTable(controller.cariPasien(txtCari.getText(), (String) cmbKriteria.getSelectedItem()));
    }

    private void tampilkanError(Exception e) {
        JOptionPane.showMessageDialog(this, "Terjadi kesalahan: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static Date toDate(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDate toLocalDate(Date d) {
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}

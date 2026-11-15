package com.klinik.view;

import com.klinik.controller.SuratController;
import com.klinik.exception.AksesDitolakException;
import com.klinik.exception.DataNotFoundException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.model.JenisSurat;
import com.klinik.model.Pemeriksaan;
import com.klinik.model.Petugas;
import com.klinik.model.Role;
import com.klinik.model.StatusSurat;
import com.klinik.model.Surat;
import com.klinik.util.HakAkses;
import com.klinik.util.SesiPengguna;
import com.klinik.view.component.TableStyleHelper;
import com.klinik.view.theme.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Antarmuka Pengelolaan Surat Keterangan Medis (Surat Izin Sakit & Surat Rujukan).
 * - Role DOKTER (BR-17): Membuat surat izin/rujukan untuk pemeriksaan miliknya sendiri
 *   yang berstatus TTV_SELESAI atau DIPERIKSA (status awal surat: DITERUSKAN).
 * - Role PETUGAS / ADMIN: Melihat daftar surat masuk dan menandai surat DITERUSKAN menjadi DISERAHKAN.
 */
public class SuratView extends JPanel {
    private static final DateTimeFormatter FORMAT_TANGGAL = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final SuratController controller;

    // Komponen Form Pembuatan Surat (Dokter)
    private JComboBox<Pemeriksaan> cmbPemeriksaan;
    private JComboBox<JenisSurat> cmbJenisSurat;
    private JSpinner spnJumlahHari;
    private JTextField txtTujuanRujukan;
    private JTextArea txtIsiSurat;
    private JLabel lblRoleInfo;

    // Tombol Aksi
    private JButton btnBuatSurat;
    private JButton btnTandaiDiserahkan;
    private JButton btnDetail;
    private JButton btnBersihkan;

    // Filter Tabel
    private JComboBox<String> cmbFilterJenis;
    private JComboBox<String> cmbFilterStatus;
    private JTextField txtCari;
    private JButton btnFilter;
    private JButton btnResetFilter;

    // Tabel Daftar Surat
    private JTable tblSurat;
    private DefaultTableModel tableModel;

    public SuratView(SuratController controller) {
        this.controller = controller;
        initComponents();
        pasangListener();
        refreshViewForCurrentSession();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        // HEADER
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel lblTitle = new JLabel("Surat Keterangan Medis (Izin Sakit & Rujukan)");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.PRIMARY);
        headerPanel.add(lblTitle, BorderLayout.WEST);

        lblRoleInfo = new JLabel();
        lblRoleInfo.setFont(UITheme.FONT_MUTED);
        lblRoleInfo.setForeground(UITheme.TEXT_MUTED);
        headerPanel.add(lblRoleInfo, BorderLayout.EAST);

        JPanel northContainer = new JPanel();
        northContainer.setLayout(new BoxLayout(northContainer, BoxLayout.Y_AXIS));
        northContainer.setOpaque(false);
        northContainer.add(headerPanel);
        northContainer.add(Box.createVerticalStrut(12));

        // FORM CARD (Untuk Dokter membuat surat)
        JPanel formCard = new JPanel(new GridLayout(1, 2, 24, 0));
        formCard.setBackground(UITheme.SURFACE);
        formCard.setBorder(UITheme.createPanelBorder());

        // Kolom Kiri Form
        JPanel leftCol = new JPanel();
        leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));
        leftCol.setOpaque(false);

        cmbPemeriksaan = new JComboBox<>();
        cmbPemeriksaan.setFont(UITheme.FONT_REGULAR);
        cmbPemeriksaan.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        cmbJenisSurat = new JComboBox<>(JenisSurat.values());
        cmbJenisSurat.setFont(UITheme.FONT_REGULAR);
        cmbJenisSurat.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        spnJumlahHari = new JSpinner(new SpinnerNumberModel(3, 1, 90, 1));
        spnJumlahHari.setFont(UITheme.FONT_REGULAR);
        spnJumlahHari.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        txtTujuanRujukan = UITheme.createTextField();
        txtTujuanRujukan.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        addField(leftCol, "Kunjungan Pemeriksaan * (BR-17: TTV_SELESAI / DIPERIKSA)", cmbPemeriksaan);
        addField(leftCol, "Jenis Surat *", cmbJenisSurat);
        addField(leftCol, "Jumlah Hari Izin * (Minimal 1 hari jika Surat Izin)", spnJumlahHari);
        addField(leftCol, "Tujuan Rujukan * (Wajib diisi jika Surat Rujukan)", txtTujuanRujukan);

        // Kolom Kanan Form
        JPanel rightCol = new JPanel();
        rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));
        rightCol.setOpaque(false);

        txtIsiSurat = new JTextArea(6, 24);
        txtIsiSurat.setFont(UITheme.FONT_REGULAR);
        txtIsiSurat.setBorder(UITheme.createFieldBorder());
        txtIsiSurat.setLineWrap(true);
        txtIsiSurat.setWrapStyleWord(true);
        JScrollPane spIsi = new JScrollPane(txtIsiSurat);
        spIsi.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));

        addField(rightCol, "Isi / Keterangan Medis Surat *", spIsi);

        formCard.add(leftCol);
        formCard.add(rightCol);
        northContainer.add(formCard);
        northContainer.add(Box.createVerticalStrut(12));

        // ACTION & FILTER TOOLBAR
        JPanel actionRow = new JPanel(new BorderLayout(8, 8));
        actionRow.setOpaque(false);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnPanel.setOpaque(false);
        btnBuatSurat = UITheme.createPrimaryButton("Buat & Teruskan Surat (Dokter)");
        btnTandaiDiserahkan = UITheme.createPrimaryButton("Tandai Diserahkan (Petugas)");
        btnDetail = UITheme.createSecondaryButton("Detail / Cetak");
        btnBersihkan = UITheme.createSecondaryButton("Bersihkan Form");

        btnPanel.add(btnBuatSurat);
        btnPanel.add(btnTandaiDiserahkan);
        btnPanel.add(btnDetail);
        btnPanel.add(btnBersihkan);
        actionRow.add(btnPanel, BorderLayout.WEST);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterPanel.setOpaque(false);
        cmbFilterJenis = new JComboBox<>(new String[]{"Semua Jenis", JenisSurat.IZIN.name(), JenisSurat.RUJUKAN.name()});
        cmbFilterJenis.setFont(UITheme.FONT_REGULAR);
        cmbFilterJenis.setPreferredSize(new Dimension(120, 30));

        cmbFilterStatus = new JComboBox<>(new String[]{"Semua Status", StatusSurat.DITERUSKAN.name(), StatusSurat.DISERAHKAN.name()});
        cmbFilterStatus.setFont(UITheme.FONT_REGULAR);
        cmbFilterStatus.setPreferredSize(new Dimension(130, 30));

        txtCari = UITheme.createTextField();
        txtCari.setPreferredSize(new Dimension(140, 30));

        btnFilter = UITheme.createSecondaryButton("Filter");
        btnResetFilter = UITheme.createSecondaryButton("Reset");

        filterPanel.add(cmbFilterJenis);
        filterPanel.add(cmbFilterStatus);
        filterPanel.add(txtCari);
        filterPanel.add(btnFilter);
        filterPanel.add(btnResetFilter);
        actionRow.add(filterPanel, BorderLayout.EAST);

        northContainer.add(actionRow);
        add(northContainer, BorderLayout.NORTH);

        // TABEL DAFTAR SURAT (CENTER)
        String[] columns = {
                "ID Surat", "ID Pemeriksaan", "Pasien", "Jenis Surat",
                "Jml Hari / Tujuan Rujukan", "Tanggal Dibuat", "Status", "Diserahkan Oleh"
        };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblSurat = new JTable(tableModel);
        TableStyleHelper.applyStyle(tblSurat);
        JScrollPane tableScroll = new JScrollPane(tblSurat);
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

    private void pasangListener() {
        cmbJenisSurat.addActionListener(e -> updateJenisSuratFields());
        btnBuatSurat.addActionListener(e -> prosesBuatSurat());
        btnTandaiDiserahkan.addActionListener(e -> prosesTandaiDiserahkan());
        btnDetail.addActionListener(e -> tampilkanDetailSurat());
        btnBersihkan.addActionListener(e -> bersihkanForm());

        btnFilter.addActionListener(e -> terapkanFilter());
        txtCari.addActionListener(e -> terapkanFilter());
        cmbFilterJenis.addActionListener(e -> terapkanFilter());
        cmbFilterStatus.addActionListener(e -> terapkanFilter());
        btnResetFilter.addActionListener(e -> {
            cmbFilterJenis.setSelectedIndex(0);
            cmbFilterStatus.setSelectedIndex(0);
            txtCari.setText("");
            refreshTable();
        });
    }

    public void refreshViewForCurrentSession() {
        Petugas current = SesiPengguna.getSession();
        Role role = current != null ? current.getRole() : Role.ADMIN;

        boolean canBuat = HakAkses.memilikiAkses(role, HakAkses.BUAT_SURAT);
        boolean canSerahkan = HakAkses.memilikiAkses(role, HakAkses.SERAHKAN_SURAT);

        cmbPemeriksaan.setEnabled(canBuat);
        cmbJenisSurat.setEnabled(canBuat);
        txtIsiSurat.setEnabled(canBuat);
        txtIsiSurat.setBackground(canBuat ? UITheme.SURFACE : UITheme.READ_ONLY_BG);
        btnBuatSurat.setEnabled(canBuat);
        btnBersihkan.setEnabled(canBuat);
        btnTandaiDiserahkan.setEnabled(canSerahkan);

        if (role == Role.DOKTER) {
            String idDkt = current != null && current.getIdDokter() != null && !current.getIdDokter().isEmpty()
                    ? current.getIdDokter() : "DKT-001";
            lblRoleInfo.setText("Mode DOKTER (" + idDkt + "): Buat surat izin/rujukan untuk pemeriksaan Anda.");
        } else if (role == Role.PETUGAS) {
            lblRoleInfo.setText("Mode PETUGAS: Pilih surat berstatus DITERUSKAN lalu klik 'Tandai Diserahkan'.");
        } else {
            lblRoleInfo.setText("Mode ADMIN: Akses penuh pembuatan dan penyerahan surat.");
        }

        if (canBuat) {
            muatPemeriksaanLayak();
        } else {
            cmbPemeriksaan.removeAllItems();
        }
        updateJenisSuratFields();
        refreshTable();
    }

    private void updateJenisSuratFields() {
        boolean canBuat = HakAkses.memilikiAkses(HakAkses.BUAT_SURAT);
        JenisSurat jenis = (JenisSurat) cmbJenisSurat.getSelectedItem();
        boolean isIzin = jenis == JenisSurat.IZIN;

        spnJumlahHari.setEnabled(canBuat && isIzin);
        txtTujuanRujukan.setEnabled(canBuat && !isIzin);
        txtTujuanRujukan.setBackground((canBuat && !isIzin) ? UITheme.SURFACE : UITheme.READ_ONLY_BG);
    }

    private void muatPemeriksaanLayak() {
        cmbPemeriksaan.removeAllItems();
        try {
            List<Pemeriksaan> layak = controller.getPemeriksaanLayakSurat();
            for (Pemeriksaan p : layak) {
                cmbPemeriksaan.addItem(p);
            }
        } catch (Exception ignored) {
        }
    }

    public void refreshTable() {
        terapkanFilter();
    }

    private void terapkanFilter() {
        String jenisStr = (String) cmbFilterJenis.getSelectedItem();
        String statusStr = (String) cmbFilterStatus.getSelectedItem();
        JenisSurat jenis = (jenisStr != null && !jenisStr.startsWith("Semua")) ? JenisSurat.fromString(jenisStr) : null;
        StatusSurat status = (statusStr != null && !statusStr.startsWith("Semua")) ? StatusSurat.fromString(statusStr) : null;

        try {
            List<Surat> hasil = controller.filterSurat(jenis, status, txtCari.getText());
            populateTable(hasil);
        } catch (AksesDitolakException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Akses Ditolak", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void populateTable(List<Surat> list) {
        tableModel.setRowCount(0);
        for (Surat s : list) {
            Pemeriksaan pmr = controller.getPemeriksaanById(s.getIdPemeriksaan());
            String namaPasien = (pmr != null && pmr.getPasien() != null) ? pmr.getPasien().getNama() : "-";
            String detailKhusus = s.getJenis() == JenisSurat.IZIN
                    ? s.getJumlahHari() + " Hari"
                    : (s.getTujuanRujukan() != null && !s.getTujuanRujukan().isEmpty() ? s.getTujuanRujukan() : "-");

            tableModel.addRow(new Object[]{
                    s.getIdSurat(),
                    s.getIdPemeriksaan(),
                    namaPasien,
                    s.getJenis() != null ? s.getJenis().getLabel() : "-",
                    detailKhusus,
                    s.getTanggalDibuat() != null ? s.getTanggalDibuat().format(FORMAT_TANGGAL) : "-",
                    s.getStatus() != null ? s.getStatus().getLabel() : "-",
                    s.getDiserahkanOleh() != null && !s.getDiserahkanOleh().isEmpty() ? s.getDiserahkanOleh() : "-"
            });
        }
    }

    private void prosesBuatSurat() {
        Pemeriksaan selectedPmr = (Pemeriksaan) cmbPemeriksaan.getSelectedItem();
        if (selectedPmr == null) {
            JOptionPane.showMessageDialog(this,
                    "Tidak ada pemeriksaan berstatus TTV_SELESAI atau DIPERIKSA yang dipilih.",
                    "Validasi Gagal", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JenisSurat jenis = (JenisSurat) cmbJenisSurat.getSelectedItem();
        int jumlahHari = (Integer) spnJumlahHari.getValue();
        String tujuan = txtTujuanRujukan.getText();
        String isi = txtIsiSurat.getText();

        try {
            Surat dibuat = controller.buatSurat(selectedPmr.getId(), jenis, isi, jumlahHari, tujuan);
            JOptionPane.showMessageDialog(this,
                    "Surat berhasil dibuat dengan ID: " + dibuat.getIdSurat() + " (Status: DITERUSKAN).",
                    "Sukses", JOptionPane.INFORMATION_MESSAGE);
            bersihkanForm();
            refreshTable();
        } catch (DataTidakValidException | DataNotFoundException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Validasi Gagal", JOptionPane.WARNING_MESSAGE);
        } catch (AksesDitolakException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Akses Ditolak", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void prosesTandaiDiserahkan() {
        int row = tblSurat.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Pilih surat berstatus DITERUSKAN pada tabel terlebih dahulu.",
                    "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String idSurat = (String) tableModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Tandai surat " + idSurat + " sebagai DISERAHKAN kepada pasien?\n"
                        + "(Surat yang sudah DISERAHKAN bersifat final dan tidak dapat diubah lagi)",
                "Konfirmasi Penyerahan Surat", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            Surat updated = controller.tandaiDiserahkan(idSurat);
            JOptionPane.showMessageDialog(this,
                    "Surat " + updated.getIdSurat() + " berhasil ditandai DISERAHKAN oleh " + updated.getDiserahkanOleh() + ".",
                    "Sukses", JOptionPane.INFORMATION_MESSAGE);
            refreshTable();
        } catch (DataTidakValidException | DataNotFoundException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Tidak Dapat Diproses", JOptionPane.WARNING_MESSAGE);
        } catch (AksesDitolakException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Akses Ditolak", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void tampilkanDetailSurat() {
        int row = tblSurat.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Pilih surat pada tabel untuk melihat detail cetak.",
                    "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String idSurat = (String) tableModel.getValueAt(row, 0);
        try {
            Surat surat = controller.getSuratById(idSurat);
            DetailDialog.show(this, "Detail Surat Keterangan Medis", surat);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void bersihkanForm() {
        cmbJenisSurat.setSelectedIndex(0);
        spnJumlahHari.setValue(3);
        txtTujuanRujukan.setText("");
        txtIsiSurat.setText("");
        tblSurat.clearSelection();
        updateJenisSuratFields();
    }
}

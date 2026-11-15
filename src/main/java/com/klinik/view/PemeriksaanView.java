package com.klinik.view;

import com.klinik.controller.PemeriksaanController;
import com.klinik.exception.KlinikException;
import com.klinik.model.Dokter;
import com.klinik.model.JenisPenjamin;
import com.klinik.model.Layanan;
import com.klinik.model.Pasien;
import com.klinik.model.Pemeriksaan;
import com.klinik.model.Petugas;
import com.klinik.model.StatusPemeriksaan;
import com.klinik.util.HakAkses;
import com.klinik.util.SesiPengguna;
import com.klinik.view.component.FormHelper;
import com.klinik.view.component.Notifikasi;
import com.klinik.view.component.TableStyleHelper;
import com.klinik.view.theme.UITheme;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
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
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * Pendaftaran kunjungan dan daftar kunjungan (FR-18, FR-22), mengikuti desain layar "Pemeriksaan".
 * Data hanya dicatat satu kali saat mendaftar; status naik otomatis saat tiap tahap disimpan di Rekam Medis.
 * Karena itu kotak "Ubah Status" pada desain diganti kotak antrean (Panggil dan Batalkan, BR-14 dan BR-15)
 * agar status tidak dapat melompat (BR-08).
 */
public class PemeriksaanView extends JPanel {
    private final PemeriksaanController controller;

    private final JTextField txtId = UITheme.createReadOnlyField();
    private final JComboBox<Pasien> cmbPasien = new JComboBox<>();
    private final JComboBox<Dokter> cmbDokter = new JComboBox<>();
    private final JComboBox<Layanan> cmbLayanan = new JComboBox<>();
    private final JRadioButton rbBpjs = new JRadioButton("BPJS");
    private final JRadioButton rbNonBpjs = new JRadioButton("Non-BPJS", true);
    private final JTextField txtDurasi = UITheme.createTextField();
    private final JSpinner spnTanggal = new JSpinner(new SpinnerDateModel());
    private final JTextArea txtKeluhan = FormHelper.area(3);
    private final JTextField txtPetugas = UITheme.createReadOnlyField();
    private final JLabel lblStatus = FormHelper.kotakInfo("-", UITheme.INFO);
    private final JLabel lblHintPenjamin = FormHelper.muted(" ");

    private final JButton btnSimpan = UITheme.createPrimaryButton("Simpan");
    private final JButton btnDetail = UITheme.createSecondaryButton("Detail");
    private final JButton btnBersihkan = UITheme.createSecondaryButton("Bersihkan");
    private final JButton btnPanggil = UITheme.createSecondaryButton("Panggil");
    private final JButton btnBatalkan = UITheme.createDangerButton("Batalkan");
    private final JLabel lblAntrean = FormHelper.muted("Pilih kunjungan pada tabel untuk memanggil atau membatalkan.");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Antrean", "Pasien", "Dokter", "Layanan", "Tanggal", "Penjamin", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tabel = new JTable(tableModel);
    private final JLabel lblTotal = FormHelper.muted("Total: 0 Data | Terpilih: -");
    private final JLabel lblBaris = FormHelper.muted(" ");

    private List<Pemeriksaan> daftar = List.of();
    private Pemeriksaan terpilih;          // null = mode pendaftaran baru

    public PemeriksaanView(PemeriksaanController controller) {
        this.controller = controller;
        susunTampilan();
        pasangListener();
        muatPilihan();
        muatTabel();
        bersihkan();
    }

    // ------------------------------------------------------------------ tampilan

    private void susunTampilan() {
        setLayout(new BorderLayout(0, 12));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        add(FormHelper.header("Pemeriksaan", "Formulir Pendaftaran & Manajemen Status Pemeriksaan Pasien",
                "Modul: Rawat Jalan / Poliklinik  •  Mode: Entri & Monitoring"), BorderLayout.NORTH);

        JPanel isi = new JPanel(new BorderLayout(0, 12));
        isi.setOpaque(false);
        isi.add(susunFormulir(), BorderLayout.NORTH);
        isi.add(susunTabel(), BorderLayout.CENTER);
        add(isi, BorderLayout.CENTER);

        tabel.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        TableStyleHelper.applyStyle(tabel);
        tabel.getColumnModel().getColumn(0).setPreferredWidth(90);
        tabel.getColumnModel().getColumn(1).setPreferredWidth(70);
        tabel.getColumnModel().getColumn(2).setPreferredWidth(170);
        tabel.getColumnModel().getColumn(3).setPreferredWidth(200);
        tabel.getColumnModel().getColumn(4).setPreferredWidth(220);
        tabel.getColumnModel().getColumn(5).setPreferredWidth(100);
        tabel.getColumnModel().getColumn(6).setPreferredWidth(90);
        tabel.getColumnModel().getColumn(7).setPreferredWidth(110);
    }

    private JPanel susunFormulir() {
        JPanel kartu = FormHelper.kartu();
        kartu.setLayout(new BorderLayout(0, 12));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        // kolom kiri
        FormHelper.baris(form, 0, 0, FormHelper.label("ID Pemeriksaan", false), txtId);
        FormHelper.baris(form, 0, 1, FormHelper.label("Pasien", true), cmbPasien);
        FormHelper.baris(form, 0, 2, FormHelper.label("Dokter", true), cmbDokter);
        FormHelper.baris(form, 0, 3, FormHelper.label("Layanan", true), cmbLayanan);
        ButtonGroup grup = new ButtonGroup();
        grup.add(rbBpjs);
        grup.add(rbNonBpjs);
        for (JRadioButton rb : new JRadioButton[]{rbBpjs, rbNonBpjs}) {
            rb.setOpaque(false);
            rb.setFont(UITheme.FONT_REGULAR);
        }
        JPanel penjamin = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        penjamin.setOpaque(false);
        penjamin.add(rbBpjs);
        penjamin.add(rbNonBpjs);
        penjamin.add(lblHintPenjamin);
        FormHelper.baris(form, 0, 4, FormHelper.label("Jenis Penjamin", true), penjamin);
        FormHelper.baris(form, 0, 5, FormHelper.label("Durasi Keluhan", true), txtDurasi);

        // kolom kanan
        spnTanggal.setEditor(new JSpinner.DateEditor(spnTanggal, "dd-MM-yyyy"));
        spnTanggal.setFont(UITheme.FONT_REGULAR);
        spnTanggal.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        FormHelper.baris(form, 2, 0, FormHelper.label("Tanggal Pemeriksaan", true), spnTanggal);
        FormHelper.baris(form, 2, 1, FormHelper.label("Keluhan", true), FormHelper.gulir(txtKeluhan));
        FormHelper.baris(form, 2, 2, FormHelper.label("Petugas", false), txtPetugas);
        FormHelper.baris(form, 2, 3, FormHelper.label("Status", false), lblStatus);

        for (JComboBox<?> c : new JComboBox<?>[]{cmbPasien, cmbDokter, cmbLayanan}) {
            c.setFont(UITheme.FONT_REGULAR);
            c.setBackground(UITheme.SURFACE);
        }
        cmbPasien.setRenderer(new PilihanRenderer());
        cmbDokter.setRenderer(new PilihanRenderer());
        cmbLayanan.setRenderer(new PilihanRenderer());

        kartu.add(form, BorderLayout.CENTER);

        // baris tombol: kiri Simpan/Detail/Bersihkan, kanan kotak antrean
        JPanel tombol = new JPanel(new BorderLayout(12, 0));
        tombol.setOpaque(false);
        tombol.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER));
        JPanel kiri = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 12));
        kiri.setOpaque(false);
        kiri.add(btnSimpan);
        kiri.add(btnDetail);
        kiri.add(btnBersihkan);
        tombol.add(kiri, BorderLayout.WEST);

        JPanel kotakAntrean = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        kotakAntrean.setBackground(UITheme.BACKGROUND);
        kotakAntrean.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        JLabel judul = new JLabel("Antrean Pasien:");
        judul.setFont(UITheme.FONT_REGULAR);
        kotakAntrean.add(judul);
        kotakAntrean.add(lblAntrean);
        kotakAntrean.add(btnPanggil);
        kotakAntrean.add(btnBatalkan);
        JPanel bungkus = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 6));
        bungkus.setOpaque(false);
        bungkus.add(kotakAntrean);
        tombol.add(bungkus, BorderLayout.EAST);

        kartu.add(tombol, BorderLayout.SOUTH);
        return kartu;
    }

    private JPanel susunTabel() {
        JPanel kartu = FormHelper.kartuTanpaPadding();

        JPanel judul = new JPanel(new BorderLayout());
        judul.setBackground(UITheme.ROW_ALT);
        judul.setBorder(new EmptyBorder(8, 12, 8, 12));
        JLabel t = new JLabel("Daftar Antrean & Transaksi Pemeriksaan");
        t.setFont(UITheme.FONT_SUBTITLE);
        t.setForeground(UITheme.TEXT);
        judul.add(t, BorderLayout.WEST);
        judul.add(lblTotal, BorderLayout.EAST);
        kartu.add(judul, BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(tabel);
        sp.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, UITheme.BORDER));
        sp.getViewport().setBackground(UITheme.SURFACE);
        kartu.add(sp, BorderLayout.CENTER);

        JPanel kaki = new JPanel(new BorderLayout());
        kaki.setBackground(UITheme.ROW_ALT);
        kaki.setBorder(new EmptyBorder(6, 12, 6, 12));
        kaki.add(lblBaris, BorderLayout.WEST);
        kaki.add(FormHelper.muted("Navigasi: [Alt+S] Simpan | [Alt+B] Bersihkan | [F5] Segarkan Tabel"), BorderLayout.EAST);
        kartu.add(kaki, BorderLayout.SOUTH);
        return kartu;
    }

    /** Menampilkan id dan nama pada daftar pilihan, sesuai desain: "PSN-001 - Budi Santoso". */
    private static class PilihanRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            String teks;
            if (value instanceof Pasien) {
                Pasien p = (Pasien) value;
                teks = p.getId() + " - " + p.getNama();
            } else if (value instanceof Dokter) {
                Dokter d = (Dokter) value;
                teks = d.getId() + " - " + d.getNama();
            } else if (value instanceof Layanan) {
                Layanan l = (Layanan) value;
                teks = l.getId() + " - " + l.getNamaLayanan();
            } else {
                teks = value == null ? "" : value.toString();
            }
            return super.getListCellRendererComponent(list, teks, index, isSelected, cellHasFocus);
        }
    }

    // ------------------------------------------------------------------ event

    private void pasangListener() {
        btnSimpan.addActionListener(e -> simpan());
        btnBersihkan.addActionListener(e -> bersihkan());
        btnDetail.addActionListener(e -> tampilkanDetail());
        btnPanggil.addActionListener(e -> panggil());
        btnBatalkan.addActionListener(e -> batalkan());
        cmbPasien.addActionListener(e -> sesuaikanPenjamin());

        tabel.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int baris = tabel.getSelectedRow();
                if (baris >= 0 && baris < daftar.size()) {
                    tampilkan(daftar.get(baris));
                }
            }
        });

        FormHelper.pintasan(this, "simpan", FormHelper.alt(KeyEvent.VK_S), this::simpan);
        FormHelper.pintasan(this, "bersihkan", FormHelper.alt(KeyEvent.VK_B), this::bersihkan);
        FormHelper.pintasan(this, "segarkan", FormHelper.f5(), () -> {
            muatPilihan();
            muatTabel();
        });
    }

    /** Memuat pilihan pasien, dokter, dan layanan aktif (BR-07). */
    public void muatPilihan() {
        try {
            isi(cmbPasien, controller.pasienAktif());
            isi(cmbDokter, controller.dokterAktif());
            isi(cmbLayanan, controller.layananAktif());
        } catch (KlinikException e) {
            Notifikasi.galat(this, e);
        }
        sesuaikanPenjamin();
    }

    private static <T> void isi(JComboBox<T> combo, List<T> data) {
        Object pilihan = combo.getSelectedItem();
        combo.removeAllItems();
        for (T t : data) combo.addItem(t);
        if (pilihan != null) combo.setSelectedItem(pilihan);
    }

    public void muatTabel() {
        String idTerpilih = terpilih != null ? terpilih.getId() : null;
        try {
            daftar = controller.semuaKunjungan();
        } catch (KlinikException e) {
            daftar = List.of();
            Notifikasi.galat(this, e);
        }
        tableModel.setRowCount(0);
        int barisPilih = -1;
        for (int i = 0; i < daftar.size(); i++) {
            Pemeriksaan p = daftar.get(i);
            tableModel.addRow(new Object[]{
                    p.getId(),
                    p.getNomorAntreanPoli() != null ? p.getNomorAntreanPoli() : "-",
                    p.getPasien() != null ? p.getPasien().getNama() : "-",
                    p.getDokter() != null ? p.getDokter().getNama() : "-",
                    p.getLayanan() != null ? p.getLayanan().getNamaLayanan() : "-",
                    p.getTanggalPemeriksaan() != null ? p.getTanggalPemeriksaan().format(FormHelper.TANGGAL) : "-",
                    p.getJenisPenjamin() != null ? p.getJenisPenjamin().getLabel() : "-",
                    p.getStatus().getLabel()});
            if (p.getId().equals(idTerpilih)) barisPilih = i;
        }
        lblTotal.setText("Total: " + daftar.size() + " Data | Terpilih: " + (idTerpilih != null ? idTerpilih : "-"));
        lblBaris.setText("Menampilkan baris " + (daftar.isEmpty() ? 0 : 1) + " sampai " + daftar.size()
                + " dari total " + daftar.size() + " record");
        if (barisPilih >= 0) {
            tabel.setRowSelectionInterval(barisPilih, barisPilih);
        }
    }

    /** BR-16: pasien tanpa nomor BPJS hanya dapat didaftarkan sebagai Non-BPJS. */
    private void sesuaikanPenjamin() {
        if (terpilih != null) return;
        Pasien p = (Pasien) cmbPasien.getSelectedItem();
        boolean punyaBpjs = p != null && p.punyaBpjs();
        rbBpjs.setEnabled(punyaBpjs);
        if (!punyaBpjs) {
            rbNonBpjs.setSelected(true);
            lblHintPenjamin.setText(p != null ? "Pasien belum punya nomor BPJS" : " ");
        } else {
            lblHintPenjamin.setText("No. BPJS " + p.getNoBpjs());
        }
    }

    private void simpan() {
        if (terpilih != null) {
            Notifikasi.peringatan(this, "Kunjungan " + terpilih.getId()
                    + " sudah tersimpan. Klik Bersihkan untuk mendaftarkan kunjungan baru.");
            return;
        }
        try {
            Pasien pasien = (Pasien) cmbPasien.getSelectedItem();
            Dokter dokter = (Dokter) cmbDokter.getSelectedItem();
            Layanan layanan = (Layanan) cmbLayanan.getSelectedItem();
            JenisPenjamin penjamin = rbBpjs.isSelected() ? JenisPenjamin.BPJS : JenisPenjamin.NON_BPJS;
            LocalDate tanggal = ((Date) spnTanggal.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            Pemeriksaan baru = controller.daftarkan(
                    pasien != null ? pasien.getId() : null,
                    dokter != null ? dokter.getId() : null,
                    layanan != null ? layanan.getId() : null,
                    tanggal, txtKeluhan.getText(), txtDurasi.getText(), penjamin);
            Notifikasi.sukses(this, "Kunjungan " + baru.getId() + " berhasil didaftarkan.\nNomor antrean poli: "
                    + baru.getNomorAntreanPoli() + " (" + baru.getDokter().getNama() + ")");
            muatTabel();
            bersihkan();
        } catch (KlinikException e) {
            Notifikasi.galat(this, e);
        }
    }

    /** Mengosongkan formulir untuk pendaftaran baru. */
    public void bersihkan() {
        terpilih = null;
        tabel.clearSelection();
        txtId.setText("(otomatis)");
        txtDurasi.setText("");
        txtKeluhan.setText("");
        spnTanggal.setValue(new Date());
        Petugas pengguna = SesiPengguna.getSession();
        txtPetugas.setText(pengguna != null ? pengguna.getNama() : "-");
        lblStatus.setText(StatusPemeriksaan.TERDAFTAR.getLabel());
        lblStatus.setForeground(UITheme.INFO);
        bolehIsi(true);
        sesuaikanPenjamin();
        perbaruiAntrean();
        lblTotal.setText("Total: " + daftar.size() + " Data | Terpilih: -");
    }

    private void tampilkan(Pemeriksaan p) {
        terpilih = p;
        txtId.setText(p.getId());
        cmbPasien.setSelectedItem(p.getPasien());
        cmbDokter.setSelectedItem(p.getDokter());
        cmbLayanan.setSelectedItem(p.getLayanan());
        rbBpjs.setSelected(p.getJenisPenjamin() == JenisPenjamin.BPJS);
        rbNonBpjs.setSelected(p.getJenisPenjamin() != JenisPenjamin.BPJS);
        txtDurasi.setText(p.getDurasiKeluhan());
        txtKeluhan.setText(p.getKeluhanAwal());
        if (p.getTanggalPemeriksaan() != null) {
            spnTanggal.setValue(Date.from(p.getTanggalPemeriksaan().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }
        txtPetugas.setText(p.getNamaPetugas() != null ? p.getNamaPetugas() : "-");
        lblStatus.setText(p.getStatus().getLabel());
        lblStatus.setForeground(warnaStatus(p.getStatus()));
        bolehIsi(false);
        perbaruiAntrean();
        lblTotal.setText("Total: " + daftar.size() + " Data | Terpilih: " + p.getId());
    }

    private static Color warnaStatus(StatusPemeriksaan s) {
        switch (s) {
            case SELESAI:     return UITheme.SUCCESS;
            case DIBATALKAN:  return UITheme.DANGER;
            case DIPERIKSA:   return UITheme.WARNING;
            default:          return UITheme.INFO;
        }
    }

    /** Mode baca (kunjungan sudah tersimpan) mengunci formulir; mode baru membukanya. */
    private void bolehIsi(boolean boleh) {
        boolean izin = boleh && HakAkses.memilikiAkses(HakAkses.DAFTAR_KUNJUNGAN);
        cmbPasien.setEnabled(izin);
        cmbDokter.setEnabled(izin);
        cmbLayanan.setEnabled(izin);
        spnTanggal.setEnabled(izin);
        rbNonBpjs.setEnabled(izin);
        rbBpjs.setEnabled(izin);
        FormHelper.aktifkan(txtDurasi, izin);
        FormHelper.aktifkan(txtKeluhan, izin);
        btnSimpan.setEnabled(izin);
    }

    private void perbaruiAntrean() {
        boolean izin = HakAkses.memilikiAkses(HakAkses.KELOLA_ANTREAN);
        boolean aktif = terpilih != null && !terpilih.getStatus().isFinal() && izin;
        btnPanggil.setEnabled(aktif);
        btnBatalkan.setEnabled(aktif);
        btnDetail.setEnabled(terpilih != null);
        if (terpilih == null) {
            lblAntrean.setText("Pilih kunjungan pada tabel untuk memanggil atau membatalkan.");
        } else if (terpilih.getStatus().isFinal()) {
            lblAntrean.setText("Kunjungan " + terpilih.getStatus().getLabel() + " (tidak dapat dipanggil).");
        } else {
            lblAntrean.setText(terpilih.getStatus().getKeterangan() + " | Dipanggil: "
                    + terpilih.getJumlahPanggilan() + "/" + Pemeriksaan.BATAS_PANGGILAN);
        }
    }

    private void panggil() {
        if (terpilih == null) return;
        try {
            Pemeriksaan p = controller.panggil(terpilih.getId());
            Notifikasi.sukses(this, "Memanggil antrean " + p.getNomorAntreanPoli() + " - " + p.getPasien().getNama()
                    + "\nPanggilan ke-" + p.getJumlahPanggilan() + " dari batas " + Pemeriksaan.BATAS_PANGGILAN + ".");
            muatTabel();
        } catch (KlinikException e) {
            Notifikasi.galat(this, e);
        }
    }

    private void batalkan() {
        if (terpilih == null) return;
        try {
            Pemeriksaan p = controller.cariKunjungan(terpilih.getId()).orElse(terpilih);
            if (!p.sudahBolehDibatalkan()) {
                Notifikasi.peringatan(this, "Kunjungan belum dapat dibatalkan: pasien baru dipanggil "
                        + p.getJumlahPanggilan() + " kali (minimal " + Pemeriksaan.BATAS_PANGGILAN + " kali).");
                return;
            }
            String alasan = JOptionPane.showInputDialog(this,
                    "Alasan pembatalan kunjungan " + p.getId() + " (wajib diisi):",
                    "Konfirmasi Pembatalan", JOptionPane.QUESTION_MESSAGE);
            if (alasan == null) return;                     // dibatalkan pengguna
            controller.batalkan(p.getId(), alasan);
            Notifikasi.sukses(this, "Kunjungan " + p.getId() + " dibatalkan karena pasien tidak hadir.");
            muatTabel();
            bersihkan();
        } catch (KlinikException e) {
            Notifikasi.galat(this, e);
        }
    }

    private void tampilkanDetail() {
        if (terpilih != null) {
            DetailDialog.show(this, "Detail Pemeriksaan", terpilih);
        }
    }
}

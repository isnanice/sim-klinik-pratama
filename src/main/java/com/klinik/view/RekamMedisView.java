package com.klinik.view;

import com.klinik.controller.RekamMedisController;
import com.klinik.exception.KlinikException;
import com.klinik.model.Pasien;
import com.klinik.model.Pemeriksaan;
import com.klinik.model.Petugas;
import com.klinik.model.RekamMedis;
import com.klinik.model.Role;
import com.klinik.model.StatusPemeriksaan;
import com.klinik.util.HakAkses;
import com.klinik.util.SesiPengguna;
import com.klinik.view.component.FormHelper;
import com.klinik.view.component.Notifikasi;
import com.klinik.view.component.TableStyleHelper;
import com.klinik.view.theme.UITheme;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Rekam medis per kunjungan (FR-19 sampai FR-21, FR-24) mengikuti desain layar "Rekam Medis".
 * Tiga panel tahap (BR-09): 1) TTV oleh petugas, 2) pemeriksaan dokter, 3) farmasi oleh petugas.
 * Hanya panel yang sesuai status kunjungan DAN role pengguna yang terbuka; panel lain terkunci
 * (menampilkan isian yang sudah ada). Penyimpanan tiap tahap sekaligus memajukan status kunjungan.
 */
public class RekamMedisView extends JPanel {
    private final RekamMedisController controller;

    // pilihan kunjungan dan ringkasan
    private final JComboBox<Pemeriksaan> cmbKunjungan = new JComboBox<>();
    private final JLabel lblPasien = nilai();
    private final JLabel lblDokter = nilai();
    private final JLabel lblTanggal = nilai();
    private final JLabel lblKeluhan = nilai();
    private final JLabel lblStatus = nilai();

    // tahap 1: TTV
    private final JSpinner spnSistolik = new JSpinner(new SpinnerNumberModel(120, 50, 300, 1));
    private final JSpinner spnDiastolik = new JSpinner(new SpinnerNumberModel(80, 30, 200, 1));
    private final JSpinner spnTinggi = new JSpinner(new SpinnerNumberModel(165.0, 30.0, 250.0, 0.5));
    private final JSpinner spnBerat = new JSpinner(new SpinnerNumberModel(60.0, 1.0, 500.0, 0.5));
    private final JButton btnSimpanTtv = UITheme.createPrimaryButton("Simpan TTV");
    private final JLabel lblTahap1 = new JLabel();

    // tahap 2: dokter
    private final JTextField txtDurasi = UITheme.createTextField();
    private final JTextArea txtHasil = FormHelper.area(3);
    private final JTextArea txtDiagnosis = FormHelper.area(3);
    private final JTextArea txtResep = FormHelper.area(3);
    private final JTextArea txtCatatan = FormHelper.area(2);
    private final JButton btnSimpanDokter = UITheme.createPrimaryButton("Simpan Hasil");
    private final JLabel lblTahap2 = new JLabel();

    // tahap 3: farmasi
    private final JTextArea txtObat = FormHelper.area(5);
    private final JButton btnSimpanFarmasi = UITheme.createPrimaryButton("Selesaikan Pelayanan");
    private final JLabel lblTahap3 = new JLabel();

    private final JButton btnBersihkan = UITheme.createSecondaryButton("Bersihkan");
    private final JButton btnDetail = UITheme.createSecondaryButton("Detail Rekam Medis");

    // riwayat per pasien
    private final JComboBox<Pasien> cmbPasienRiwayat = new JComboBox<>();
    private final DefaultTableModel modelRiwayat = new DefaultTableModel(
            new String[]{"Tanggal", "Dokter", "Diagnosis", "Resep", "Obat Diberikan"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tabelRiwayat = new JTable(modelRiwayat);
    private final JLabel lblTotalRiwayat = FormHelper.muted("Total 0 Kunjungan");
    private final JLabel lblBarisRiwayat = FormHelper.muted(" ");

    private List<RekamMedis> riwayat = new ArrayList<>();
    private Pemeriksaan kunjungan;           // kunjungan yang sedang ditampilkan
    private RekamMedis rekamMedis;           // rekam medis kunjungan itu (null bila belum ada TTV)
    private boolean sedangMemuat;

    public RekamMedisView(RekamMedisController controller) {
        this.controller = controller;
        susunTampilan();
        pasangListener();
        segarkan();
    }

    private static JLabel nilai() {
        JLabel l = new JLabel("-");
        l.setFont(UITheme.FONT_REGULAR);
        l.setForeground(UITheme.TEXT);
        return l;
    }

    // ------------------------------------------------------------------ tampilan

    private void susunTampilan() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BACKGROUND);

        JPanel isi = new JPanel();
        isi.setLayout(new javax.swing.BoxLayout(isi, javax.swing.BoxLayout.Y_AXIS));
        isi.setBackground(UITheme.BACKGROUND);
        isi.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel header = FormHelper.header("Rekam Medis", "| Modul Pencatatan TTV, Diagnosis Klinis & Farmasi", "Alur: TTV → Dokter → Farmasi");
        header.setAlignmentX(LEFT_ALIGNMENT);
        isi.add(header);
        isi.add(javax.swing.Box.createVerticalStrut(12));
        isi.add(rata(susunKartuKunjungan()));
        isi.add(javax.swing.Box.createVerticalStrut(12));
        isi.add(rata(susunPanelTahap()));
        isi.add(javax.swing.Box.createVerticalStrut(8));
        isi.add(rata(susunTombol()));
        isi.add(javax.swing.Box.createVerticalStrut(12));
        isi.add(rata(susunRiwayat()));

        JScrollPane gulir = new JScrollPane(isi);
        gulir.setBorder(null);
        gulir.getVerticalScrollBar().setUnitIncrement(16);
        gulir.getViewport().setBackground(UITheme.BACKGROUND);
        add(gulir, BorderLayout.CENTER);
    }

    private static JPanel rata(JPanel p) {
        p.setAlignmentX(LEFT_ALIGNMENT);
        return p;
    }

    private JPanel susunKartuKunjungan() {
        JPanel kartu = FormHelper.kartu();
        kartu.setLayout(new GridBagLayout());

        // kiri: pilih kunjungan
        JPanel kiri = new JPanel(new GridBagLayout());
        kiri.setOpaque(false);
        cmbKunjungan.setFont(UITheme.FONT_REGULAR);
        cmbKunjungan.setBackground(UITheme.SURFACE);
        cmbKunjungan.setRenderer(new KunjunganRenderer());
        FormHelper.baris(kiri, 0, 0, FormHelper.label("Pemeriksaan", true), cmbKunjungan);
        GridBagConstraints petunjuk = new GridBagConstraints();
        petunjuk.gridx = 1;
        petunjuk.gridy = 1;
        petunjuk.anchor = GridBagConstraints.NORTHWEST;
        petunjuk.insets = new Insets(0, 0, 0, 0);
        kiri.add(FormHelper.muted("Hanya kunjungan yang belum SELESAI atau DIBATALKAN yang tampil."), petunjuk);

        // kanan: ringkasan
        JPanel ringkas = new JPanel(new GridBagLayout());
        ringkas.setBackground(UITheme.BACKGROUND);
        ringkas.setBorder(new javax.swing.border.CompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1), new EmptyBorder(8, 12, 8, 12)));
        JLabel judul = new JLabel("Ringkasan Pemeriksaan Terpilih");
        judul.setFont(UITheme.FONT_SUBTITLE);
        judul.setForeground(UITheme.PRIMARY);
        GridBagConstraints gj = new GridBagConstraints();
        gj.gridx = 0;
        gj.gridy = 0;
        gj.gridwidth = 2;
        gj.anchor = GridBagConstraints.WEST;
        gj.insets = new Insets(0, 0, 6, 0);
        ringkas.add(judul, gj);
        baris(ringkas, 1, "Pasien", lblPasien);
        baris(ringkas, 2, "Dokter", lblDokter);
        baris(ringkas, 3, "Tanggal", lblTanggal);
        baris(ringkas, 4, "Keluhan", lblKeluhan);
        baris(ringkas, 5, "Status", lblStatus);

        GridBagConstraints gk = new GridBagConstraints();
        gk.gridx = 0;
        gk.gridy = 0;
        gk.weightx = 0.45;
        gk.fill = GridBagConstraints.HORIZONTAL;
        gk.anchor = GridBagConstraints.NORTH;
        gk.insets = new Insets(0, 0, 0, 16);
        kartu.add(kiri, gk);
        GridBagConstraints gr = new GridBagConstraints();
        gr.gridx = 1;
        gr.gridy = 0;
        gr.weightx = 0.55;
        gr.fill = GridBagConstraints.HORIZONTAL;
        gr.anchor = GridBagConstraints.NORTH;
        kartu.add(ringkas, gr);
        return kartu;
    }

    private static void baris(JPanel p, int y, String nama, JLabel isi) {
        GridBagConstraints a = new GridBagConstraints();
        a.gridx = 0;
        a.gridy = y;
        a.anchor = GridBagConstraints.NORTHWEST;
        a.insets = new Insets(1, 0, 1, 12);
        p.add(FormHelper.muted(nama), a);
        GridBagConstraints b = new GridBagConstraints();
        b.gridx = 1;
        b.gridy = y;
        b.weightx = 1.0;
        b.fill = GridBagConstraints.HORIZONTAL;
        b.anchor = GridBagConstraints.NORTHWEST;
        b.insets = new Insets(1, 0, 1, 0);
        p.add(isi, b);
    }

    private JPanel susunPanelTahap() {
        JPanel baris = new JPanel(new GridLayout(1, 3, 12, 0));
        baris.setOpaque(false);
        baris.add(panelTahap("1. Pemeriksaan Awal (TTV)", "Petugas", lblTahap1, formTtv(), btnSimpanTtv));
        baris.add(panelTahap("2. Pemeriksaan Dokter", "Dokter", lblTahap2, formDokter(), btnSimpanDokter));
        baris.add(panelTahap("3. Farmasi", "Petugas", lblTahap3, formFarmasi(), btnSimpanFarmasi));
        return baris;
    }

    private JPanel panelTahap(String judul, String oleh, JLabel lblStatusTahap, JPanel isi, JButton simpan) {
        JPanel kartu = new JPanel(new BorderLayout(0, 8));
        kartu.setBackground(UITheme.SURFACE);
        kartu.setBorder(UITheme.createPanelBorder());

        JPanel atas = new JPanel(new BorderLayout());
        atas.setOpaque(false);
        JLabel t = new JLabel(judul);
        t.setFont(UITheme.FONT_SUBTITLE);
        t.setForeground(UITheme.TEXT);
        atas.add(t, BorderLayout.WEST);
        lblStatusTahap.setFont(UITheme.FONT_BOLD);
        atas.add(lblStatusTahap, BorderLayout.EAST);
        JPanel judulBox = new JPanel(new BorderLayout());
        judulBox.setOpaque(false);
        judulBox.add(atas, BorderLayout.NORTH);
        judulBox.add(FormHelper.muted("Diisi oleh " + oleh), BorderLayout.SOUTH);
        kartu.add(judulBox, BorderLayout.NORTH);

        kartu.add(isi, BorderLayout.CENTER);

        JPanel bawah = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bawah.setOpaque(false);
        bawah.add(simpan);
        kartu.add(bawah, BorderLayout.SOUTH);
        return kartu;
    }

    private JPanel formTtv() {
        JPanel f = new JPanel(new GridBagLayout());
        f.setOpaque(false);
        for (JSpinner s : new JSpinner[]{spnSistolik, spnDiastolik, spnTinggi, spnBerat}) {
            s.setFont(UITheme.FONT_REGULAR);
            s.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        }
        FormHelper.baris(f, 0, 0, FormHelper.label("Tensi Sistolik (mmHg)", true), spnSistolik);
        FormHelper.baris(f, 0, 1, FormHelper.label("Tensi Diastolik (mmHg)", true), spnDiastolik);
        FormHelper.baris(f, 0, 2, FormHelper.label("Tinggi Badan (cm)", true), spnTinggi);
        FormHelper.baris(f, 0, 3, FormHelper.label("Berat Badan (kg)", true), spnBerat);
        // pengisi agar panel tidak merenggang
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 4;
        g.weighty = 1.0;
        f.add(new JPanel() {{ setOpaque(false); }}, g);
        return f;
    }

    private JPanel formDokter() {
        JPanel f = new JPanel(new GridBagLayout());
        f.setOpaque(false);
        FormHelper.baris(f, 0, 0, FormHelper.label("Durasi Keluhan", false), txtDurasi);
        FormHelper.baris(f, 0, 1, FormHelper.label("Hasil Periksa", true), FormHelper.gulir(txtHasil));
        FormHelper.baris(f, 0, 2, FormHelper.label("Diagnosis", true), FormHelper.gulir(txtDiagnosis));
        FormHelper.baris(f, 0, 3, FormHelper.label("Resep & Obat", true), FormHelper.gulir(txtResep));
        FormHelper.baris(f, 0, 4, FormHelper.label("Catatan Khusus", false), FormHelper.gulir(txtCatatan));
        return f;
    }

    private JPanel formFarmasi() {
        JPanel f = new JPanel(new BorderLayout(0, 6));
        f.setOpaque(false);
        f.add(FormHelper.label("Obat yang Diberikan", true), BorderLayout.NORTH);
        f.add(FormHelper.gulir(txtObat), BorderLayout.CENTER);
        f.add(FormHelper.muted("Sesuaikan dengan resep dokter (tanpa master obat)."), BorderLayout.SOUTH);
        return f;
    }

    private JPanel susunTombol() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        p.setOpaque(false);
        p.add(btnDetail);
        p.add(btnBersihkan);
        return p;
    }

    private JPanel susunRiwayat() {
        JPanel kartu = FormHelper.kartuTanpaPadding();

        JPanel judul = new JPanel(new BorderLayout());
        judul.setBackground(UITheme.ROW_ALT);
        judul.setBorder(new EmptyBorder(8, 12, 8, 12));
        JPanel kiri = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        kiri.setOpaque(false);
        JLabel t = new JLabel("Riwayat per Pasien");
        t.setFont(UITheme.FONT_SUBTITLE);
        kiri.add(t);
        kiri.add(lblTotalRiwayat);
        judul.add(kiri, BorderLayout.WEST);
        JPanel kanan = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        kanan.setOpaque(false);
        JLabel pilih = new JLabel("Pilih Pasien:");
        pilih.setFont(UITheme.FONT_BOLD);
        kanan.add(pilih);
        cmbPasienRiwayat.setFont(UITheme.FONT_REGULAR);
        cmbPasienRiwayat.setRenderer(new PasienRenderer());
        cmbPasienRiwayat.setPreferredSize(new Dimension(260, 28));
        kanan.add(cmbPasienRiwayat);
        judul.add(kanan, BorderLayout.EAST);
        kartu.add(judul, BorderLayout.NORTH);

        tabelRiwayat.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        TableStyleHelper.applyStyle(tabelRiwayat);
        JScrollPane sp = new JScrollPane(tabelRiwayat);
        sp.setPreferredSize(new Dimension(100, 28 * 5 + 36));
        sp.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, UITheme.BORDER));
        sp.getViewport().setBackground(UITheme.SURFACE);
        kartu.add(sp, BorderLayout.CENTER);

        JPanel kaki = new JPanel(new BorderLayout());
        kaki.setBackground(UITheme.ROW_ALT);
        kaki.setBorder(new EmptyBorder(6, 12, 6, 12));
        kaki.add(lblBarisRiwayat, BorderLayout.WEST);
        kaki.add(FormHelper.muted("Klik satu baris untuk melihat rekam medisnya | [Alt+B] Bersihkan | [F5] Segarkan"), BorderLayout.EAST);
        kartu.add(kaki, BorderLayout.SOUTH);
        return kartu;
    }

    private static class KunjunganRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            String teks = "";
            if (value instanceof Pemeriksaan) {
                Pemeriksaan p = (Pemeriksaan) value;
                teks = p.getId() + " - " + (p.getPasien() != null ? p.getPasien().getNama() : "-")
                        + " (" + (p.getNomorAntreanPoli() != null ? p.getNomorAntreanPoli() : "-") + ") · "
                        + p.getStatus().getLabel();
            }
            return super.getListCellRendererComponent(list, teks, index, isSelected, cellHasFocus);
        }
    }

    private static class PasienRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            String teks = value instanceof Pasien ? ((Pasien) value).getId() + " - " + ((Pasien) value).getNama() : "";
            return super.getListCellRendererComponent(list, teks, index, isSelected, cellHasFocus);
        }
    }

    // ------------------------------------------------------------------ event

    private void pasangListener() {
        cmbKunjungan.addActionListener(e -> {
            if (!sedangMemuat) {
                Pemeriksaan p = (Pemeriksaan) cmbKunjungan.getSelectedItem();
                tampilkanKunjungan(p);
            }
        });
        cmbPasienRiwayat.addActionListener(e -> {
            if (!sedangMemuat) muatRiwayat();
        });
        tabelRiwayat.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int baris = tabelRiwayat.getSelectedRow();
                if (baris >= 0 && baris < riwayat.size()) {
                    RekamMedis rm = riwayat.get(baris);
                    tampilkanKunjungan(rm.getPemeriksaan());
                }
            }
        });
        btnSimpanTtv.addActionListener(e -> simpanTtv());
        btnSimpanDokter.addActionListener(e -> simpanDokter());
        btnSimpanFarmasi.addActionListener(e -> simpanFarmasi());
        btnBersihkan.addActionListener(e -> bersihkan());
        btnDetail.addActionListener(e -> {
            if (rekamMedis != null) DetailDialog.show(this, "Detail Rekam Medis", rekamMedis);
        });
        FormHelper.pintasan(this, "bersihkan", FormHelper.alt(KeyEvent.VK_B), this::bersihkan);
        FormHelper.pintasan(this, "segarkan", FormHelper.f5(), this::segarkan);
    }

    /** Memuat ulang pilihan kunjungan, pasien, dan riwayat. */
    public void segarkan() {
        sedangMemuat = true;
        try {
            String idLama = kunjungan != null ? kunjungan.getId() : null;
            cmbKunjungan.removeAllItems();
            Pemeriksaan pilih = null;
            for (Pemeriksaan p : kunjunganYangBolehDilihat()) {
                cmbKunjungan.addItem(p);
                if (p.getId().equals(idLama)) pilih = p;
            }
            if (pilih != null) cmbKunjungan.setSelectedItem(pilih); else cmbKunjungan.setSelectedItem(null);

            Pasien pasienLama = (Pasien) cmbPasienRiwayat.getSelectedItem();
            cmbPasienRiwayat.removeAllItems();
            for (Pasien p : controller.semuaPasien()) cmbPasienRiwayat.addItem(p);
            if (pasienLama != null) cmbPasienRiwayat.setSelectedItem(pasienLama);
        } catch (KlinikException e) {
            Notifikasi.galat(this, e);
        } finally {
            sedangMemuat = false;
        }
        tampilkanKunjungan((Pemeriksaan) cmbKunjungan.getSelectedItem());
        muatRiwayat();
    }

    /** Dokter hanya melihat kunjungan pasiennya sendiri; admin dan petugas melihat semua yang berjalan. */
    private List<Pemeriksaan> kunjunganYangBolehDilihat() {
        List<Pemeriksaan> hasil = new ArrayList<>();
        try {
            Petugas pengguna = SesiPengguna.getSession();
            for (Pemeriksaan p : controller.kunjunganBerjalan()) {
                if (pengguna != null && pengguna.getRole() == Role.DOKTER
                        && !(p.getDokter() != null && p.getDokter().getId().equalsIgnoreCase(pengguna.getIdDokter()))) {
                    continue;
                }
                hasil.add(p);
            }
        } catch (KlinikException e) {
            Notifikasi.galat(this, e);
        }
        return hasil;
    }

    private void muatRiwayat() {
        riwayat = new ArrayList<>();
        Pasien pasien = (Pasien) cmbPasienRiwayat.getSelectedItem();
        if (pasien != null) {
            try {
                riwayat = controller.riwayatPerPasien(pasien.getId());
            } catch (KlinikException e) {
                Notifikasi.galat(this, e);
            }
        }
        modelRiwayat.setRowCount(0);
        for (RekamMedis rm : riwayat) {
            modelRiwayat.addRow(new Object[]{
                    rm.getTanggal() != null ? rm.getTanggal().format(FormHelper.TANGGAL) : "-",
                    rm.getDokter() != null ? rm.getDokter().getNama() : "-",
                    rm.getDiagnosis() != null ? rm.getDiagnosis() : "(belum diisi dokter)",
                    rm.getResep() != null ? rm.getResep() : "-",
                    rm.getObatDiberikan() != null ? rm.getObatDiberikan() : "-"});
        }
        lblTotalRiwayat.setText("Total " + riwayat.size() + " Kunjungan");
        lblBarisRiwayat.setText("Menampilkan " + riwayat.size() + " rekam medis"
                + (pasien != null ? " untuk " + pasien.getNama() : ""));
    }

    /** Menampilkan satu kunjungan: ringkasan, isian tiap tahap, dan panel mana yang terbuka. */
    private void tampilkanKunjungan(Pemeriksaan p) {
        kunjungan = p;
        rekamMedis = null;
        if (p != null) {
            try {
                rekamMedis = controller.getRekamMedisByPemeriksaanId(p.getId()).orElse(null);
            } catch (KlinikException e) {
                Notifikasi.galat(this, e);
            }
        }
        isiRingkasan(p);
        isiField();
        atur();
    }

    private void isiRingkasan(Pemeriksaan p) {
        if (p == null) {
            for (JLabel l : new JLabel[]{lblPasien, lblDokter, lblTanggal, lblKeluhan, lblStatus}) l.setText("-");
            lblStatus.setForeground(UITheme.TEXT);
            return;
        }
        lblPasien.setText(p.getPasien().getId() + " - " + p.getPasien().getNama()
                + " (" + (p.getJenisPenjamin() != null ? p.getJenisPenjamin().getLabel() : "-") + ")");
        lblDokter.setText(p.getDokter().getNama());
        lblTanggal.setText(p.getTanggalPemeriksaan().format(FormHelper.TANGGAL)
                + "  •  Antrean " + (p.getNomorAntreanPoli() != null ? p.getNomorAntreanPoli() : "-"));
        lblKeluhan.setText("<html>" + escape(p.getKeluhanAwal()) + " (" + escape(p.getDurasiKeluhan()) + ")</html>");
        lblStatus.setText(p.getStatus().getLabel() + " - " + p.getStatus().getKeterangan());
        lblStatus.setForeground(p.getStatus() == StatusPemeriksaan.SELESAI ? UITheme.SUCCESS
                : p.getStatus() == StatusPemeriksaan.DIBATALKAN ? UITheme.DANGER : UITheme.INFO);
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void isiField() {
        RekamMedis rm = rekamMedis;
        spnSistolik.setValue(rm != null && rm.getTensiSistolik() != null ? rm.getTensiSistolik() : 120);
        spnDiastolik.setValue(rm != null && rm.getTensiDiastolik() != null ? rm.getTensiDiastolik() : 80);
        spnTinggi.setValue(rm != null && rm.getTinggiBadanCm() != null ? rm.getTinggiBadanCm() : 165.0);
        spnBerat.setValue(rm != null && rm.getBeratBadanKg() != null ? rm.getBeratBadanKg() : 60.0);
        txtDurasi.setText(rm != null && rm.getDurasiKeluhan() != null ? rm.getDurasiKeluhan()
                : (kunjungan != null && kunjungan.getDurasiKeluhan() != null ? kunjungan.getDurasiKeluhan() : ""));
        txtHasil.setText(rm != null && rm.getHasilPemeriksaan() != null ? rm.getHasilPemeriksaan() : "");
        txtDiagnosis.setText(rm != null && rm.getDiagnosis() != null ? rm.getDiagnosis() : "");
        txtResep.setText(rm != null && rm.getResep() != null ? rm.getResep() : "");
        txtCatatan.setText(rm != null && rm.getCatatan() != null ? rm.getCatatan() : "");
        txtObat.setText(rm != null && rm.getObatDiberikan() != null ? rm.getObatDiberikan() : "");
    }

    /** BR-09: satu panel terbuka sesuai status kunjungan, dan hanya bila role pengguna berwenang. */
    private void atur() {
        StatusPemeriksaan s = kunjungan != null ? kunjungan.getStatus() : null;
        boolean bolehTtv = s == StatusPemeriksaan.TERDAFTAR && HakAkses.memilikiAkses(HakAkses.ISI_TTV);
        boolean bolehDokter = s == StatusPemeriksaan.TTV_SELESAI && HakAkses.memilikiAkses(HakAkses.PERIKSA_DOKTER)
                && dokterBerwenang();
        boolean bolehFarmasi = s == StatusPemeriksaan.DIPERIKSA && HakAkses.memilikiAkses(HakAkses.ISI_FARMASI);

        for (JSpinner sp : new JSpinner[]{spnSistolik, spnDiastolik, spnTinggi, spnBerat}) sp.setEnabled(bolehTtv);
        btnSimpanTtv.setEnabled(bolehTtv);
        FormHelper.aktifkan(txtDurasi, bolehDokter);
        for (JTextArea a : new JTextArea[]{txtHasil, txtDiagnosis, txtResep, txtCatatan}) FormHelper.aktifkan(a, bolehDokter);
        btnSimpanDokter.setEnabled(bolehDokter);
        FormHelper.aktifkan(txtObat, bolehFarmasi);
        btnSimpanFarmasi.setEnabled(bolehFarmasi);
        btnDetail.setEnabled(rekamMedis != null);

        badge(lblTahap1, s, StatusPemeriksaan.TERDAFTAR, rekamMedis != null && rekamMedis.punyaTtv());
        badge(lblTahap2, s, StatusPemeriksaan.TTV_SELESAI, rekamMedis != null && rekamMedis.punyaPemeriksaanDokter());
        badge(lblTahap3, s, StatusPemeriksaan.DIPERIKSA, rekamMedis != null && rekamMedis.punyaFarmasi());
    }

    private static void badge(JLabel l, StatusPemeriksaan sekarang, StatusPemeriksaan tahapIni, boolean terisi) {
        if (terisi) {
            l.setText("Selesai");
            l.setForeground(UITheme.SUCCESS);
        } else if (sekarang == tahapIni) {
            l.setText("Giliran tahap ini");
            l.setForeground(UITheme.PRIMARY);
        } else if (sekarang == StatusPemeriksaan.DIBATALKAN) {
            l.setText("Dibatalkan");
            l.setForeground(UITheme.DANGER);
        } else {
            l.setText("Terkunci");
            l.setForeground(UITheme.TEXT_MUTED);
        }
    }

    /** Petunjuk tampilan saja; pemeriksaan sebenarnya ada di PemeriksaanController. */
    private boolean dokterBerwenang() {
        Petugas pengguna = SesiPengguna.getSession();
        if (pengguna == null || pengguna.getRole() != Role.DOKTER) return true;
        return kunjungan != null && kunjungan.getDokter() != null
                && kunjungan.getDokter().getId().equalsIgnoreCase(pengguna.getIdDokter());
    }

    private void bersihkan() {
        sedangMemuat = true;
        cmbKunjungan.setSelectedItem(null);
        tabelRiwayat.clearSelection();
        sedangMemuat = false;
        tampilkanKunjungan(null);
    }

    // ------------------------------------------------------------------ simpan per tahap

    private void simpanTtv() {
        if (kunjungan == null) return;
        try {
            controller.simpanTtv(kunjungan.getId(),
                    ((Number) spnSistolik.getValue()).intValue(), ((Number) spnDiastolik.getValue()).intValue(),
                    ((Number) spnTinggi.getValue()).doubleValue(), ((Number) spnBerat.getValue()).doubleValue());
            Notifikasi.sukses(this, "TTV tersimpan. Status kunjungan " + kunjungan.getId() + " menjadi TTV_SELESAI.");
            segarkan();
        } catch (KlinikException e) {
            Notifikasi.galat(this, e);
        }
    }

    private void simpanDokter() {
        if (kunjungan == null) return;
        try {
            controller.simpanPemeriksaanDokter(kunjungan.getId(), txtDurasi.getText(), txtHasil.getText(),
                    txtDiagnosis.getText(), txtResep.getText(), txtCatatan.getText());
            Notifikasi.sukses(this, "Hasil pemeriksaan tersimpan. Status kunjungan " + kunjungan.getId() + " menjadi DIPERIKSA.");
            segarkan();
        } catch (KlinikException e) {
            Notifikasi.galat(this, e);
        }
    }

    private void simpanFarmasi() {
        if (kunjungan == null) return;
        try {
            String id = kunjungan.getId();
            controller.simpanFarmasi(id, txtObat.getText());
            Notifikasi.sukses(this, "Pelayanan " + id + " selesai. Kunjungan siap dibayar di menu Pembayaran.");
            kunjungan = null;
            segarkan();
        } catch (KlinikException e) {
            Notifikasi.galat(this, e);
        }
    }
}

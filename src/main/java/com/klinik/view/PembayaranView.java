package com.klinik.view;

import com.klinik.controller.PembayaranController;
import com.klinik.exception.KlinikException;
import com.klinik.model.Layanan;
import com.klinik.model.MetodePembayaran;
import com.klinik.model.Pembayaran;
import com.klinik.model.Pemeriksaan;
import com.klinik.model.StatusPembayaran;
import com.klinik.util.HakAkses;
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
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Pembayaran (FR-25 sampai FR-28) mengikuti desain layar "Pembayaran". Hanya kunjungan SELESAI yang
 * belum dibayar yang dapat dipilih (BR-10). Total dihitung polimorfik lewat controller. Pembayaran
 * LUNAS terkunci; yang BELUM_LUNAS masih dapat diubah statusnya.
 */
public class PembayaranView extends JPanel {
    private final PembayaranController controller;

    private final JComboBox<Pemeriksaan> cmbKunjungan = new JComboBox<>();
    private final JLabel lblPasien = nilai();
    private final JLabel lblLayanan = nilai();
    private final JLabel lblRincian = nilai();
    private final JLabel lblPenjamin = nilai();
    private final JTextField txtBiayaTambahan = UITheme.createTextField();
    private final JComboBox<StatusPembayaran> cmbStatus = new JComboBox<>(StatusPembayaran.values());
    private final JRadioButton rbTunai = new JRadioButton("Tunai", true);
    private final JRadioButton rbTransfer = new JRadioButton("Transfer");
    private final JLabel lblTotal = new JLabel("Rp 0");
    private final JLabel lblTerakhir = FormHelper.muted("Pembayaran terakhir: -");

    private final JButton btnHitung = UITheme.createSecondaryButton("Hitung Total");
    private final JButton btnSimpan = UITheme.createPrimaryButton("Simpan");
    private final JButton btnUbahStatus = UITheme.createSecondaryButton("Ubah Status");
    private final JButton btnDetail = UITheme.createSecondaryButton("Detail");
    private final JButton btnBersihkan = UITheme.createSecondaryButton("Bersihkan");

    private final DefaultTableModel modelRiwayat = new DefaultTableModel(
            new String[]{"ID", "Pemeriksaan", "Pasien", "Layanan", "Tanggal", "Biaya Tambahan", "Total", "Metode", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tabel = new JTable(modelRiwayat);
    private final JLabel lblJumlah = FormHelper.muted("Total: 0 Data");
    private final JLabel lblBaris = FormHelper.muted(" ");

    private List<Pembayaran> riwayat = new ArrayList<>();
    private Pembayaran terpilih;            // pembayaran tersimpan yang sedang dilihat (null = transaksi baru)
    private boolean sedangMemuat;

    public PembayaranView(PembayaranController controller) {
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
        setLayout(new BorderLayout(0, 12));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        add(FormHelper.header("Pembayaran", "Transaksi Pembayaran Layanan Pemeriksaan",
                "Hanya kunjungan berstatus SELESAI yang dapat dibayar"), BorderLayout.NORTH);

        JPanel isi = new JPanel(new BorderLayout(0, 12));
        isi.setOpaque(false);
        isi.add(susunForm(), BorderLayout.NORTH);
        isi.add(susunRiwayat(), BorderLayout.CENTER);
        add(isi, BorderLayout.CENTER);
    }

    private JPanel susunForm() {
        JPanel kartu = FormHelper.kartu();
        kartu.setLayout(new BorderLayout(0, 12));

        // kiri: isian
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        cmbKunjungan.setFont(UITheme.FONT_REGULAR);
        cmbKunjungan.setBackground(UITheme.SURFACE);
        cmbKunjungan.setRenderer(new KunjunganRenderer());
        cmbStatus.setFont(UITheme.FONT_REGULAR);
        cmbStatus.setBackground(UITheme.SURFACE);
        FormHelper.baris(form, 0, 0, FormHelper.label("Pemeriksaan", true), cmbKunjungan);
        FormHelper.baris(form, 0, 1, FormHelper.label("Biaya Tambahan (Rp)", false), txtBiayaTambahan);
        FormHelper.baris(form, 0, 2, FormHelper.label("Status Pembayaran", true), cmbStatus);
        ButtonGroup grup = new ButtonGroup();
        grup.add(rbTunai);
        grup.add(rbTransfer);
        JPanel metode = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        metode.setOpaque(false);
        for (JRadioButton rb : new JRadioButton[]{rbTunai, rbTransfer}) {
            rb.setOpaque(false);
            rb.setFont(UITheme.FONT_REGULAR);
            metode.add(rb);
        }
        FormHelper.baris(form, 0, 3, FormHelper.label("Metode Pembayaran", true), metode);

        // tengah: ringkasan
        JPanel ringkas = new JPanel(new GridBagLayout());
        ringkas.setBackground(UITheme.BACKGROUND);
        ringkas.setBorder(new javax.swing.border.CompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1), new EmptyBorder(8, 12, 8, 12)));
        JLabel judul = new JLabel("Ringkasan Pemeriksaan");
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
        baris(ringkas, 2, "Layanan", lblLayanan);
        baris(ringkas, 3, "Rincian Tarif", lblRincian);
        baris(ringkas, 4, "Penjamin", lblPenjamin);

        // kanan: kotak total
        JPanel total = new JPanel(new BorderLayout(0, 4));
        total.setBackground(UITheme.PRIMARY);
        total.setBorder(new EmptyBorder(14, 20, 14, 20));
        JLabel cap = new JLabel("TOTAL BIAYA");
        cap.setFont(UITheme.FONT_BOLD);
        cap.setForeground(java.awt.Color.WHITE);
        lblTotal.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 26));
        lblTotal.setForeground(java.awt.Color.WHITE);
        total.add(cap, BorderLayout.NORTH);
        total.add(lblTotal, BorderLayout.CENTER);
        total.setPreferredSize(new Dimension(230, 90));

        JPanel tengah = new JPanel(new GridBagLayout());
        tengah.setOpaque(false);
        GridBagConstraints a = new GridBagConstraints();
        a.gridx = 0;
        a.weightx = 0.42;
        a.fill = GridBagConstraints.HORIZONTAL;
        a.anchor = GridBagConstraints.NORTH;
        a.insets = new Insets(0, 0, 0, 16);
        tengah.add(form, a);
        GridBagConstraints b = new GridBagConstraints();
        b.gridx = 1;
        b.weightx = 0.40;
        b.fill = GridBagConstraints.HORIZONTAL;
        b.anchor = GridBagConstraints.NORTH;
        b.insets = new Insets(0, 0, 0, 16);
        tengah.add(ringkas, b);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 2;
        c.weightx = 0.18;
        c.anchor = GridBagConstraints.NORTHEAST;
        tengah.add(total, c);
        kartu.add(tengah, BorderLayout.CENTER);

        JPanel tombol = new JPanel(new BorderLayout());
        tombol.setOpaque(false);
        tombol.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER));
        JPanel kiri = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 12));
        kiri.setOpaque(false);
        kiri.add(btnHitung);
        kiri.add(btnSimpan);
        kiri.add(btnUbahStatus);
        kiri.add(btnDetail);
        kiri.add(btnBersihkan);
        tombol.add(kiri, BorderLayout.WEST);
        JPanel kanan = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 18));
        kanan.setOpaque(false);
        kanan.add(lblTerakhir);
        tombol.add(kanan, BorderLayout.EAST);
        kartu.add(tombol, BorderLayout.SOUTH);
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

    private JPanel susunRiwayat() {
        JPanel kartu = FormHelper.kartuTanpaPadding();
        JPanel judul = new JPanel(new BorderLayout());
        judul.setBackground(UITheme.ROW_ALT);
        judul.setBorder(new EmptyBorder(8, 12, 8, 12));
        JLabel t = new JLabel("Riwayat Pembayaran");
        t.setFont(UITheme.FONT_SUBTITLE);
        judul.add(t, BorderLayout.WEST);
        judul.add(lblJumlah, BorderLayout.EAST);
        kartu.add(judul, BorderLayout.NORTH);

        tabel.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        TableStyleHelper.applyStyle(tabel);
        JScrollPane sp = new JScrollPane(tabel);
        sp.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, UITheme.BORDER));
        sp.getViewport().setBackground(UITheme.SURFACE);
        kartu.add(sp, BorderLayout.CENTER);

        JPanel kaki = new JPanel(new BorderLayout());
        kaki.setBackground(UITheme.ROW_ALT);
        kaki.setBorder(new EmptyBorder(6, 12, 6, 12));
        kaki.add(lblBaris, BorderLayout.WEST);
        kaki.add(FormHelper.muted("Navigasi: [Alt+S] Simpan | [Alt+B] Bersihkan | [F5] Segarkan"), BorderLayout.EAST);
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
                        + " · " + (p.getLayanan() != null ? p.getLayanan().getNamaLayanan() : "-");
            }
            return super.getListCellRendererComponent(list, teks, index, isSelected, cellHasFocus);
        }
    }

    // ------------------------------------------------------------------ event

    private void pasangListener() {
        cmbKunjungan.addActionListener(e -> {
            if (!sedangMemuat) {
                terpilih = null;
                tabel.clearSelection();
                isiRingkasan((Pemeriksaan) cmbKunjungan.getSelectedItem());
                hitung(false);
                atur();
            }
        });
        btnHitung.addActionListener(e -> hitung(true));
        btnSimpan.addActionListener(e -> simpan());
        btnUbahStatus.addActionListener(e -> ubahStatus());
        btnBersihkan.addActionListener(e -> bersihkan());
        btnDetail.addActionListener(e -> {
            if (terpilih != null) DetailDialog.show(this, "Detail Pembayaran", terpilih);
        });
        tabel.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int baris = tabel.getSelectedRow();
                if (baris >= 0 && baris < riwayat.size()) tampilkan(riwayat.get(baris));
            }
        });
        FormHelper.pintasan(this, "simpan", FormHelper.alt(KeyEvent.VK_S), this::simpan);
        FormHelper.pintasan(this, "bersihkan", FormHelper.alt(KeyEvent.VK_B), this::bersihkan);
        FormHelper.pintasan(this, "segarkan", FormHelper.f5(), this::segarkan);
    }

    /** Memuat ulang pilihan kunjungan siap bayar dan tabel riwayat. */
    public void segarkan() {
        sedangMemuat = true;
        try {
            cmbKunjungan.removeAllItems();
            for (Pemeriksaan p : controller.kunjunganSiapBayar()) cmbKunjungan.addItem(p);
            cmbKunjungan.setSelectedItem(null);
            riwayat = controller.getAllPembayaran();
        } catch (KlinikException e) {
            riwayat = new ArrayList<>();
            Notifikasi.galat(this, e);
        } finally {
            sedangMemuat = false;
        }
        modelRiwayat.setRowCount(0);
        for (Pembayaran b : riwayat) {
            Pemeriksaan p = b.getPemeriksaan();
            modelRiwayat.addRow(new Object[]{
                    b.getId(),
                    p != null ? p.getId() : "-",
                    p != null && p.getPasien() != null ? p.getPasien().getNama() : "-",
                    p != null && p.getLayanan() != null ? p.getLayanan().getNamaLayanan() : "-",
                    b.getTanggalPembayaran() != null ? b.getTanggalPembayaran().format(FormHelper.TANGGAL) : "-",
                    Pembayaran.formatRupiah(b.getBiayaTambahan()),
                    Pembayaran.formatRupiah(b.getTotalBiaya()),
                    b.getMetodePembayaran().getLabel(),
                    b.getStatusPembayaran().getLabel()});
        }
        lblJumlah.setText("Total: " + riwayat.size() + " Data");
        lblBaris.setText("Menampilkan " + riwayat.size() + " pembayaran");
        lblTerakhir.setText("Pembayaran terakhir: " + (riwayat.isEmpty() ? "-" : riwayat.get(0).getId()));
        bersihkanForm();
    }

    private void isiRingkasan(Pemeriksaan p) {
        if (p == null) {
            for (JLabel l : new JLabel[]{lblPasien, lblLayanan, lblRincian, lblPenjamin}) l.setText("-");
            return;
        }
        Layanan l = p.getLayanan();
        lblPasien.setText(p.getPasien().getId() + " - " + p.getPasien().getNama());
        lblLayanan.setText(l.getNamaLayanan() + " (" + l.getJenisLayanan() + ")");
        lblRincian.setText("<html>" + l.rincianTarif().replace("&", "&amp;").replace("<", "&lt;") + "</html>");
        lblPenjamin.setText(p.getJenisPenjamin() != null ? p.getJenisPenjamin().getLabel() : "-");
    }

    /** Membaca biaya tambahan; kosong dianggap 0. */
    private double bacaBiayaTambahan() {
        String s = txtBiayaTambahan.getText() == null ? "" : txtBiayaTambahan.getText().trim();
        if (s.isEmpty()) return 0;
        try {
            return Double.parseDouble(s.replace(".", "").replace(",", "."));
        } catch (NumberFormatException e) {
            throw new com.klinik.exception.DataTidakValidException("Biaya Tambahan", "Biaya tambahan harus berupa angka.");
        }
    }

    private void hitung(boolean tampilkanGalat) {
        Pemeriksaan p = (Pemeriksaan) cmbKunjungan.getSelectedItem();
        if (p == null) {
            if (terpilih == null) lblTotal.setText("Rp 0");
            return;
        }
        try {
            lblTotal.setText(Pembayaran.formatRupiah(controller.hitungTotalBiaya(p.getLayanan(), bacaBiayaTambahan())));
        } catch (KlinikException e) {
            lblTotal.setText("Rp 0");
            if (tampilkanGalat) Notifikasi.galat(this, e);
        }
    }

    private void simpan() {
        if (terpilih != null) {
            Notifikasi.peringatan(this, "Pembayaran " + terpilih.getId()
                    + " sudah tersimpan. Gunakan Ubah Status atau klik Bersihkan untuk transaksi baru.");
            return;
        }
        Pemeriksaan p = (Pemeriksaan) cmbKunjungan.getSelectedItem();
        try {
            if (p == null) {
                Notifikasi.peringatan(this, "Pilih kunjungan yang akan dibayar terlebih dahulu.");
                return;
            }
            MetodePembayaran metode = rbTransfer.isSelected() ? MetodePembayaran.TRANSFER : MetodePembayaran.TUNAI;
            Pembayaran b = controller.prosesPembayaran(p.getId(), bacaBiayaTambahan(), metode,
                    (StatusPembayaran) cmbStatus.getSelectedItem());
            Notifikasi.sukses(this, "Pembayaran " + b.getId() + " tersimpan.\nTotal: "
                    + Pembayaran.formatRupiah(b.getTotalBiaya()) + " (" + b.getStatusPembayaran().getLabel() + ")");
            segarkan();
        } catch (KlinikException e) {
            Notifikasi.galat(this, e);
        }
    }

    private void ubahStatus() {
        if (terpilih == null) {
            Notifikasi.peringatan(this, "Pilih pembayaran pada tabel terlebih dahulu.");
            return;
        }
        try {
            controller.updateStatusPembayaran(terpilih.getId(), (StatusPembayaran) cmbStatus.getSelectedItem());
            Notifikasi.sukses(this, "Status pembayaran " + terpilih.getId() + " diperbarui.");
            segarkan();
        } catch (KlinikException e) {
            Notifikasi.galat(this, e);
        }
    }

    private void bersihkan() {
        tabel.clearSelection();
        segarkan();
    }

    private void bersihkanForm() {
        terpilih = null;
        sedangMemuat = true;
        cmbKunjungan.setSelectedItem(null);
        sedangMemuat = false;
        txtBiayaTambahan.setText("0");
        cmbStatus.setSelectedItem(StatusPembayaran.LUNAS);
        rbTunai.setSelected(true);
        lblTotal.setText("Rp 0");
        isiRingkasan(null);
        atur();
    }

    /** Menampilkan pembayaran tersimpan dari tabel (formulir terkunci, hanya status yang dapat diubah). */
    private void tampilkan(Pembayaran b) {
        terpilih = b;
        sedangMemuat = true;
        cmbKunjungan.setSelectedItem(null);
        sedangMemuat = false;
        isiRingkasan(b.getPemeriksaan());
        txtBiayaTambahan.setText(String.valueOf(Math.round(b.getBiayaTambahan())));
        cmbStatus.setSelectedItem(b.getStatusPembayaran());
        rbTunai.setSelected(b.getMetodePembayaran() == MetodePembayaran.TUNAI);
        rbTransfer.setSelected(b.getMetodePembayaran() == MetodePembayaran.TRANSFER);
        lblTotal.setText(Pembayaran.formatRupiah(b.getTotalBiaya()));
        atur();
    }

    private void atur() {
        boolean izin = HakAkses.memilikiAkses(HakAkses.KELOLA_PEMBAYARAN);
        boolean baru = terpilih == null;
        boolean bisaUbah = !baru && terpilih.getStatusPembayaran() != StatusPembayaran.LUNAS && izin;
        cmbKunjungan.setEnabled(baru && izin);
        FormHelper.aktifkan(txtBiayaTambahan, baru && izin);
        rbTunai.setEnabled(baru && izin);
        rbTransfer.setEnabled(baru && izin);
        cmbStatus.setEnabled((baru && izin) || bisaUbah);
        btnHitung.setEnabled(baru && izin);
        btnSimpan.setEnabled(baru && izin);
        btnUbahStatus.setEnabled(bisaUbah);
        btnDetail.setEnabled(!baru);
    }
}

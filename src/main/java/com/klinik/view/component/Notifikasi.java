package com.klinik.view.component;

import com.klinik.exception.AksesDitolakException;
import com.klinik.exception.DataMasihDipakaiException;
import com.klinik.exception.DataNotFoundException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.exception.DatabaseException;

import javax.swing.JOptionPane;
import java.awt.Component;

/**
 * Dialog standar aplikasi (katalog dialog di desain): satu tempat memetakan exception ke JOptionPane
 * agar semua view memberi pesan yang sama dan informatif, bukan stack trace.
 */
public final class Notifikasi {
    private Notifikasi() { }

    public static void galat(Component induk, Exception e) {
        String judul;
        int jenis;
        if (e instanceof AksesDitolakException) {
            judul = "Akses Ditolak";
            jenis = JOptionPane.ERROR_MESSAGE;
        } else if (e instanceof DataMasihDipakaiException) {
            judul = "Data Tidak Dapat Dihapus";
            jenis = JOptionPane.WARNING_MESSAGE;
        } else if (e instanceof DataTidakValidException || e instanceof DataNotFoundException) {
            judul = "Validasi Gagal";
            jenis = JOptionPane.WARNING_MESSAGE;
        } else if (e instanceof DatabaseException) {
            judul = "Kesalahan Database";
            jenis = JOptionPane.ERROR_MESSAGE;
        } else {
            judul = "Terjadi Kesalahan";
            jenis = JOptionPane.ERROR_MESSAGE;
        }
        JOptionPane.showMessageDialog(induk, e.getMessage() != null ? e.getMessage() : e.toString(), judul, jenis);
    }

    public static void sukses(Component induk, String pesan) {
        JOptionPane.showMessageDialog(induk, pesan, "Berhasil", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void peringatan(Component induk, String pesan) {
        JOptionPane.showMessageDialog(induk, pesan, "Peringatan", JOptionPane.WARNING_MESSAGE);
    }

    public static boolean konfirmasi(Component induk, String pesan, String judul) {
        return JOptionPane.showConfirmDialog(induk, pesan, judul,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }
}

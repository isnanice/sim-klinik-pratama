package com.klinik.model;

import com.klinik.exception.DataTidakValidException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Entitas Pasien dalam Sistem Informasi Manajemen Klinik Pratama.
 * Mewarisi Orang dan mengimplementasikan DapatDicetak.
 */
public class Pasien extends Orang implements DapatDicetak {
    private String nik;
    private LocalDate tanggalLahir;
    private String noBpjs;
    private LocalDate tanggalRegistrasi;

    public Pasien() {
        super();
        this.nik = "";
        this.tanggalRegistrasi = LocalDate.now();
    }

    public Pasien(String id, String nik, String nama, JenisKelamin jenisKelamin, LocalDate tanggalLahir,
                  String alamat, String noTelepon, String noBpjs) {
        this(id, nik, nama, jenisKelamin, tanggalLahir, alamat, noTelepon, noBpjs, LocalDate.now(), true);
    }

    public Pasien(String id, String nama, JenisKelamin jenisKelamin, LocalDate tanggalLahir,
                  String alamat, String noTelepon, String noBpjs, LocalDate tanggalRegistrasi, boolean aktif) {
        this(id, "", nama, jenisKelamin, tanggalLahir, alamat, noTelepon, noBpjs, tanggalRegistrasi, aktif);
    }

    public Pasien(String id, String nik, String nama, JenisKelamin jenisKelamin, LocalDate tanggalLahir,
                  String alamat, String noTelepon, String noBpjs, LocalDate tanggalRegistrasi, boolean aktif) {
        super(id, nama, alamat, noTelepon);
        this.jenisKelamin = jenisKelamin;
        this.aktif = aktif;
        this.nik = nik != null ? nik.trim() : "";
        this.tanggalLahir = tanggalLahir;
        this.noBpjs = noBpjs != null ? noBpjs.trim() : "";
        this.tanggalRegistrasi = tanggalRegistrasi != null ? tanggalRegistrasi : LocalDate.now();
    }

    public String getNik() {
        return nik;
    }

    public void setNik(String nik) throws DataTidakValidException {
        if (nik == null || nik.trim().isEmpty()) {
            this.nik = "";
            return;
        }
        String cleanNik = nik.trim();
        if (!cleanNik.matches("^[0-9]{16}$")) {
            throw new DataTidakValidException("NIK", "NIK harus terdiri dari 16 digit angka.");
        }
        this.nik = cleanNik;
    }

    public LocalDate getTanggalLahir() {
        return tanggalLahir;
    }

    public void setTanggalLahir(LocalDate tanggalLahir) throws DataTidakValidException {
        if (tanggalLahir == null) {
            throw new DataTidakValidException("Tanggal Lahir", "Tanggal lahir wajib diisi.");
        }
        if (tanggalLahir.isAfter(LocalDate.now())) {
            throw new DataTidakValidException("Tanggal Lahir", "Tanggal lahir tidak boleh di masa depan.");
        }
        this.tanggalLahir = tanggalLahir;
    }

    public String getNoBpjs() {
        return noBpjs;
    }

    public void setNoBpjs(String noBpjs) throws DataTidakValidException {
        if (noBpjs == null || noBpjs.trim().isEmpty()) {
            this.noBpjs = "";
            return;
        }
        String cleanBpjs = noBpjs.trim();
        if (!cleanBpjs.matches("^[0-9]{13}$")) {
            throw new DataTidakValidException("No. BPJS", "Nomor BPJS harus terdiri dari 13 digit angka (atau kosongkan jika tidak ada).");
        }
        this.noBpjs = cleanBpjs;
    }

    public LocalDate getTanggalRegistrasi() {
        return tanggalRegistrasi;
    }

    public void setTanggalRegistrasi(LocalDate tanggalRegistrasi) throws DataTidakValidException {
        if (tanggalRegistrasi == null) {
            throw new DataTidakValidException("Tanggal Registrasi", "Tanggal registrasi wajib diisi.");
        }
        this.tanggalRegistrasi = tanggalRegistrasi;
    }

    public boolean punyaBpjs() {
        return noBpjs != null && !noBpjs.trim().isEmpty();
    }

    public boolean isStatusAktif() {
        return isAktif();
    }

    public void setStatusAktif(boolean statusAktif) {
        setAktif(statusAktif);
    }

    @Override
    public String getPeran() {
        return "Pasien";
    }

    @Override
    public void tampilkanInfo() {
        System.out.println(cetakRingkasan());
    }

    @Override
    public String cetakRingkasan() {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-18s: %s%n", "ID Pasien", id != null ? id : "-"));
        sb.append(String.format("%-18s: %s%n", "NIK", nik != null && !nik.isEmpty() ? nik : "-"));
        sb.append(String.format("%-18s: %s%n", "Nama", nama != null ? nama : "-"));
        sb.append(String.format("%-18s: %s%n", "Jenis Kelamin", jenisKelamin != null ? jenisKelamin.getLabel() : "-"));
        sb.append(String.format("%-18s: %s%n", "Tgl Lahir", tanggalLahir != null ? tanggalLahir.format(df) : "-"));
        sb.append(String.format("%-18s: %s%n", "No. Telepon", noTelepon != null ? noTelepon : "-"));
        sb.append(String.format("%-18s: %s%n", "No. BPJS", noBpjs != null && !noBpjs.isEmpty() ? noBpjs : "-"));
        sb.append(String.format("%-18s: %s%n", "Alamat", alamat != null && !alamat.isEmpty() ? alamat : "-"));
        sb.append(String.format("%-18s: %s%n", "Tgl Registrasi", tanggalRegistrasi != null ? tanggalRegistrasi.format(df) : "-"));
        sb.append(String.format("%-18s: %s%n", "Status", aktif ? "Aktif" : "Nonaktif"));
        return sb.toString();
    }

    @Override
    public String toString() {
        return id + " - " + nama;
    }
}

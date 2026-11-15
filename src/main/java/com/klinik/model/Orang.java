package com.klinik.model;

import com.klinik.exception.DataTidakValidException;

import java.util.Objects;

/**
 * Kelas abstrak Orang untuk hierarki entitas individu dalam klinik (Pasien, Dokter, Petugas).
 */
public abstract class Orang implements DapatDicetak {
    protected String id;
    protected String nama;
    protected JenisKelamin jenisKelamin;
    protected String alamat;
    protected String noTelepon;
    protected boolean aktif;

    public Orang() {
        this.aktif = true;
    }

    public Orang(String id, String nama, JenisKelamin jenisKelamin, String alamat, String noTelepon, boolean aktif) {
        this.id = id;
        this.nama = nama;
        this.jenisKelamin = jenisKelamin;
        this.alamat = alamat;
        this.noTelepon = noTelepon;
        this.aktif = aktif;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) throws DataTidakValidException {
        if (id != null && id.trim().isEmpty()) {
            throw new DataTidakValidException("ID", "ID tidak boleh berupa string kosong.");
        }
        this.id = id != null ? id.trim() : null;
    }

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) throws DataTidakValidException {
        if (nama == null || nama.trim().isEmpty()) {
            throw new DataTidakValidException("Nama", "Nama wajib diisi dan tidak boleh kosong.");
        }
        this.nama = nama.trim();
    }

    public JenisKelamin getJenisKelamin() {
        return jenisKelamin;
    }

    public void setJenisKelamin(JenisKelamin jenisKelamin) throws DataTidakValidException {
        if (jenisKelamin == null) {
            throw new DataTidakValidException("Jenis Kelamin", "Jenis kelamin wajib dipilih.");
        }
        this.jenisKelamin = jenisKelamin;
    }

    public String getAlamat() {
        return alamat;
    }

    public void setAlamat(String alamat) {
        this.alamat = alamat != null ? alamat.trim() : "";
    }

    public String getNoTelepon() {
        return noTelepon;
    }

    public void setNoTelepon(String noTelepon) throws DataTidakValidException {
        if (noTelepon == null || noTelepon.trim().isEmpty()) {
            throw new DataTidakValidException("No. Telepon", "Nomor telepon wajib diisi.");
        }
        this.noTelepon = noTelepon.trim();
    }

    public boolean isAktif() {
        return aktif;
    }

    public void setAktif(boolean aktif) {
        this.aktif = aktif;
    }

    public abstract String getPeran();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Orang orang = (Orang) o;
        return Objects.equals(id, orang.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

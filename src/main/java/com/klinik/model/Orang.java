package com.klinik.model;

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

    public void setId(String id) {
        this.id = id;
    }

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    public JenisKelamin getJenisKelamin() {
        return jenisKelamin;
    }

    public void setJenisKelamin(JenisKelamin jenisKelamin) {
        this.jenisKelamin = jenisKelamin;
    }

    public String getAlamat() {
        return alamat;
    }

    public void setAlamat(String alamat) {
        this.alamat = alamat;
    }

    public String getNoTelepon() {
        return noTelepon;
    }

    public void setNoTelepon(String noTelepon) {
        this.noTelepon = noTelepon;
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


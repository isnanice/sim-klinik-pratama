package com.klinik.controller;

import com.klinik.dao.DokterDAO;
import com.klinik.dao.PemeriksaanDAO;
import com.klinik.exception.AksesDitolakException;
import com.klinik.exception.DataMasihDipakaiException;
import com.klinik.exception.DataNotFoundException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.model.Dokter;
import com.klinik.model.Role;
import com.klinik.validation.DokterValidator;

import java.util.List;

/**
 * Controller untuk mengelola operasi bisnis entitas Dokter.
 */
public class DokterController {
    private final DokterDAO dokterDAO;
    private final PemeriksaanDAO pemeriksaanDAO;
    private Role currentRole;

    public DokterController(DokterDAO dokterDAO, PemeriksaanDAO pemeriksaanDAO) {
        this(dokterDAO, pemeriksaanDAO, Role.ADMIN);
    }

    public DokterController(DokterDAO dokterDAO, PemeriksaanDAO pemeriksaanDAO, Role currentRole) {
        this.dokterDAO = dokterDAO;
        this.pemeriksaanDAO = pemeriksaanDAO;
        this.currentRole = currentRole != null ? currentRole : Role.ADMIN;
    }

    public Role getCurrentRole() {
        return currentRole;
    }

    public void setCurrentRole(Role currentRole) {
        this.currentRole = currentRole;
    }

    private void checkAdminAccess(String action) throws AksesDitolakException {
        if (currentRole != Role.ADMIN) {
            throw new AksesDitolakException("Role PETUGAS tidak dapat " + action + " data dokter. Hanya dapat melihat data.");
        }
    }

    public List<Dokter> getAllDokter() {
        return dokterDAO.findAll();
    }

    public List<Dokter> getActiveDokter() {
        return dokterDAO.findActive();
    }

    public Dokter getDokterById(String id) throws DataNotFoundException {
        return dokterDAO.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Dokter dengan ID " + id + " tidak ditemukan."));
    }

    public void tambahDokter(Dokter dokter) throws DataTidakValidException, AksesDitolakException {
        checkAdminAccess("menambah");
        DokterValidator.validate(dokter);
        dokterDAO.insert(dokter);
    }

    public void ubahDokter(Dokter dokter) throws DataTidakValidException, AksesDitolakException, DataNotFoundException {
        checkAdminAccess("mengubah");
        if (dokter.getId() == null || !dokterDAO.findById(dokter.getId()).isPresent()) {
            throw new DataNotFoundException("Dokter yang akan diubah tidak ditemukan.");
        }
        DokterValidator.validate(dokter);
        dokterDAO.update(dokter);
    }

    /**
     * BR-11: Hapus permanen hanya jika dokter belum punya riwayat pemeriksaan.
     * Jika punya, lemparkan DataMasihDipakaiException.
     */
    public void hapusDokter(String id) throws AksesDitolakException, DataMasihDipakaiException, DataNotFoundException {
        checkAdminAccess("menghapus");
        Dokter d = getDokterById(id);
        if (pemeriksaanDAO != null && pemeriksaanDAO.existsByDokterId(id)) {
            throw new DataMasihDipakaiException("Dokter", id,
                    "Dokter " + d.getNama() + " (" + id + ") memiliki riwayat pemeriksaan dan tidak dapat dihapus permanen. Gunakan fitur Nonaktifkan.");
        }
        dokterDAO.delete(id);
    }

    public void nonaktifkanDokter(String id, boolean aktif) throws AksesDitolakException, DataNotFoundException {
        checkAdminAccess(aktif ? "mengaktifkan" : "menonaktifkan");
        getDokterById(id);
        dokterDAO.toggleAktif(id, aktif);
    }

    public List<Dokter> cariDokter(String keyword, String criteria) {
        return dokterDAO.search(keyword, criteria);
    }
}


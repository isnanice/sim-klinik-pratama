package com.klinik.controller;

import com.klinik.dao.LayananDAO;
import com.klinik.exception.AksesDitolakException;
import com.klinik.exception.DataNotFoundException;
import com.klinik.exception.DataTidakValidException;
import com.klinik.model.Layanan;
import com.klinik.model.Role;
import com.klinik.validation.LayananValidator;

import java.util.List;

/**
 * Controller untuk mengelola operasi bisnis entitas Layanan (LayananUmum dan LayananSpesialis).
 */
public class LayananController {
    private final LayananDAO layananDAO;
    private Role currentRole;

    public LayananController(LayananDAO layananDAO) {
        this(layananDAO, Role.ADMIN);
    }

    public LayananController(LayananDAO layananDAO, Role currentRole) {
        this.layananDAO = layananDAO;
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
            throw new AksesDitolakException("Role PETUGAS tidak dapat " + action + " data layanan. Hanya dapat melihat data.");
        }
    }

    public List<Layanan> getAllLayanan() {
        return layananDAO.findAll();
    }

    public List<Layanan> getActiveLayanan() {
        return layananDAO.findActive();
    }

    public Layanan getLayananById(String id) throws DataNotFoundException {
        return layananDAO.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Layanan dengan ID " + id + " tidak ditemukan."));
    }

    public void tambahLayanan(Layanan layanan) throws DataTidakValidException, AksesDitolakException {
        checkAdminAccess("menambah");
        LayananValidator.validate(layanan);
        layananDAO.insert(layanan);
    }

    public void ubahLayanan(Layanan layanan) throws DataTidakValidException, AksesDitolakException, DataNotFoundException {
        checkAdminAccess("mengubah");
        if (layanan.getId() == null || !layananDAO.findById(layanan.getId()).isPresent()) {
            throw new DataNotFoundException("Layanan yang akan diubah tidak ditemukan.");
        }
        LayananValidator.validate(layanan);
        layananDAO.update(layanan);
    }

    /**
     * Layanan tidak dihapus permanen, hanya dinonaktifkan (FR-15, DESIGN.md C5).
     */
    public void nonaktifkanLayanan(String id, boolean aktif) throws AksesDitolakException, DataNotFoundException {
        checkAdminAccess(aktif ? "mengaktifkan" : "menonaktifkan");
        getLayananById(id);
        layananDAO.toggleAktif(id, aktif);
    }

    public List<Layanan> cariLayanan(String keyword) {
        return layananDAO.search(keyword);
    }
}


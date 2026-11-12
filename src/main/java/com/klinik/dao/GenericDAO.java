package com.klinik.dao;

import java.util.List;
import java.util.Optional;

/**
 * Interface DAO generik untuk operasi CRUD entitas.
 *
 * @param <T> Tipe entitas model
 * @param <ID> Tipe pengidentifikasi (misalnya String)
 */
public interface GenericDAO<T, ID> {
    void insert(T entity);
    void update(T entity);
    void delete(ID id);
    Optional<T> findById(ID id);
    List<T> findAll();
}


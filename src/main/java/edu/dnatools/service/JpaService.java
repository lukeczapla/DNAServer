package edu.dnatools.service;

import java.io.Serializable;
import java.util.List;

/**
 * Created by luke on 6/3/16.
 */
public interface JpaService<T, ID extends Serializable> {
    List<T> getAll();
    T getOne(ID id);
    T add(T entity);
    T update(ID id, T entity);
    void delete(ID id);
    void deleteAll();
}

package edu.dnatools.service;


import edu.dnatools.model.IDable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.Serializable;
import java.util.List;

/**
 * Created by luke on 6/3/16.
 */
@Transactional
public abstract class AbstractService<T extends IDable<ID>, ID extends Serializable> implements JpaService<T, ID> {
    @PersistenceContext
    protected EntityManager em;

    protected abstract JpaRepository<T, ID> getDao();

    @Override
    public List<T> getAll() {
        return getDao().findAll();
    }

    @Override
    public T getOne(ID id) {
        return checkExistsAndGet(id);
    }

    protected final T checkExistsAndGet(ID id) {
        return checkFound(getDao().getOne(id));
    }

    @Override
    public T add(T entity) {
        if(entity.getId() != null && getDao().existsById(entity.getId()))
            throw new RuntimeException("Element with this Id already exists");
        T t = getDao().saveAndFlush(entity);

        return t;
    }

    @Override
    public void delete(ID id) {
        getDao().deleteById(id);
    }

    @Override
    public void deleteAll() {
        getDao().deleteAll();
    }

    public static <T> T checkFound(final T resource) {
        if (resource == null) {
            throw new RuntimeException("Element not found");
        }

        return resource;
    }

    protected final T updateReplacementId(ID id, T entity) {
        checkExistsAndGet(id);
        entity.setId(id);
        return getDao().saveAndFlush(entity);
    }
}

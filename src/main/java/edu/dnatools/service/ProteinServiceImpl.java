package edu.dnatools.service;

import edu.dnatools.model.Protein;
import edu.dnatools.repository.ProteinRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by luke on 6/10/17.
 */
@Service
@Transactional
public class ProteinServiceImpl extends AbstractService<Protein, Long> implements ProteinService {
    @Autowired
    ProteinRepository proteinRepository;

    @Override
    protected JpaRepository<Protein, Long> getDao() {
        return proteinRepository;
    }

    @Override
    public List<Protein> getIdAndNameOnly() {
        return proteinRepository.getProteinIdAndName();
    }


    @Override
    public Protein add(Protein entity) {
        return super.add(entity);
    }

    @Override
    public Protein update(Long id, Protein newProtein) {
        Protein oldProtein = getOne(id);
        newProtein.setId(id);

        return getDao().saveAndFlush(newProtein);
    }

}

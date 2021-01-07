package edu.dnatools.service;

import edu.dnatools.model.ProteinStructure;
import edu.dnatools.repository.ProteinStructureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by luke on 6/17/17.
 */
@Service
@Transactional
public class ProteinStructureServiceImpl extends AbstractService<ProteinStructure, Long> implements ProteinStructureService {

    @Autowired
    ProteinStructureRepository proteinStructureRepository;


    @Override
    protected JpaRepository<ProteinStructure, Long> getDao() {
        return proteinStructureRepository;
    }

    @Override
    public List<ProteinStructure> getIdAndNameOnly() {
        return proteinStructureRepository.getProteinStructureIdAndName();
    }

    @Override
    public ProteinStructure add(ProteinStructure entity) {
        return super.add(entity);
    }

    @Override
    public ProteinStructure update(Long id, ProteinStructure newProtein) {
        ProteinStructure oldProtein = getOne(id);
        newProtein.setId(id);

        return getDao().saveAndFlush(newProtein);
    }

}

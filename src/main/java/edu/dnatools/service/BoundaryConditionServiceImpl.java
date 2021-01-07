package edu.dnatools.service;

import edu.dnatools.model.BoundaryCondition;
import edu.dnatools.repository.BoundaryConditionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by luke on 6/13/17.
 */
@Service
@Transactional
public class BoundaryConditionServiceImpl extends AbstractService<BoundaryCondition, Long> implements BoundaryConditionService {

    @Autowired
    private BoundaryConditionRepository boundaryConditionRepository;

    @Override
    protected JpaRepository<BoundaryCondition, Long> getDao() {
        return boundaryConditionRepository;
    }

    @Override
    public BoundaryCondition add(BoundaryCondition entity) {
        return super.add(entity);
    }

    @Override
    public BoundaryCondition update(Long id, BoundaryCondition newBoundaryCondition) {
        BoundaryCondition oldBoundaryCondition = getOne(id);
        newBoundaryCondition.setId(id);

        return getDao().saveAndFlush(newBoundaryCondition);
    }

}

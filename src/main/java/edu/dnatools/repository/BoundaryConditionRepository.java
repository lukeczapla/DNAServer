package edu.dnatools.repository;

import edu.dnatools.model.BoundaryCondition;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by luke on 6/13/17.
 */
public interface BoundaryConditionRepository extends JpaRepository<BoundaryCondition, Long> {

}

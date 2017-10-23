package edu.dnatools.repository;

import edu.dnatools.model.ProteinStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Created by luke on 6/17/17.
 */
public interface ProteinStructureRepository extends JpaRepository<ProteinStructure, Long> {

    @Query("select p.id, p.name, p.description from ProteinStructure p")
    List<ProteinStructure> getProteinStructureIdAndName();
}

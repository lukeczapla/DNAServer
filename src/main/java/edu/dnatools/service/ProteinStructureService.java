package edu.dnatools.service;

import edu.dnatools.model.ProteinStructure;

import java.util.List;

/**
 * Created by luke on 6/17/17.
 */
public interface ProteinStructureService extends JpaService<ProteinStructure, Long> {
    List<ProteinStructure> getIdAndNameOnly();
}

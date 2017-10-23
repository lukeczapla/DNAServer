package edu.dnatools.service;

import edu.dnatools.model.Protein;

import java.util.List;

/**
 * Created by luke on 6/10/17.
 */
public interface ProteinService extends JpaService<Protein, Long> {
    List<Protein> getIdAndNameOnly();
}

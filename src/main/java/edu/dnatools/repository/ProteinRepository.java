package edu.dnatools.repository;

import edu.dnatools.model.Protein;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Created by luke on 6/10/17.
 */
public interface ProteinRepository extends JpaRepository<Protein, Long> {

    @Query("select p.id, p.name from Protein p")
    List<Protein> getProteinIdAndName();

}
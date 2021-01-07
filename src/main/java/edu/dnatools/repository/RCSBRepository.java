package edu.dnatools.repository;

import edu.dnatools.model.RCSBAnalysis;
import edu.dnatools.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created by luke on 7/22/17.
 */
public interface RCSBRepository extends JpaRepository<RCSBAnalysis, Long> {
    List<RCSBAnalysis> findByUser(User user);
}

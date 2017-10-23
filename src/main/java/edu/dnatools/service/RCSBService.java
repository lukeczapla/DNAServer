package edu.dnatools.service;

import edu.dnatools.model.RCSBAnalysis;
import edu.dnatools.model.User;
import edu.dnatools.repository.RCSBRepository;

import java.util.List;

/**
 * Created by luke on 7/22/17.
 */
public interface RCSBService extends JpaService<RCSBAnalysis, Long> {
    List<RCSBAnalysis> getRCSBAnalysisByUser(User user);
}

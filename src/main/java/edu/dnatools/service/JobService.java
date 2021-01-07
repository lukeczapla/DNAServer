package edu.dnatools.service;

import edu.dnatools.model.Job;
import edu.dnatools.model.User;

import java.util.List;

/**
 * Created by luke on 6/10/17.
 */
public interface JobService extends JpaService<Job, Long> {
    List<Job> getJobsByUser(User user);
    Job getJobByToken(String token);
}

package edu.dnatools.repository;

import edu.dnatools.model.Job;
import edu.dnatools.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created by luke on 6/10/17.
 */
public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByUser(User user);
    Job findByToken(String token);
}

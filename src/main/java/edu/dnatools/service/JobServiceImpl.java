package edu.dnatools.service;

import edu.dnatools.model.Job;
import edu.dnatools.model.User;
import edu.dnatools.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by luke on 6/10/17.
 */
@Service
@Transactional
public class JobServiceImpl extends AbstractService<Job, Long> implements JobService {
    @Autowired
    private JobRepository jobRepository;

    @Override
    public List<Job> getJobsByUser(User user) {
        return jobRepository.findByUser(user);
    }

    @Override
    public Job getJobByToken(String token) {
        return jobRepository.findByToken(token);
    }

    @Override
    protected JpaRepository<Job, Long> getDao() {
        return jobRepository;
    }

    @Override
    public Job add(Job entity) {
        return super.add(entity);
    }

    @Override
    public Job update(Long id, Job newJob) {
        Job oldJob = getOne(id);
        newJob.setId(id);

        return getDao().saveAndFlush(newJob);
    }


}

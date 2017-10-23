package edu.dnatools.service;

import edu.dnatools.model.RCSBAnalysis;
import edu.dnatools.model.User;
import edu.dnatools.repository.RCSBRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by luke on 7/22/17.
 */
@Service
@Transactional
public class RCSBServiceImpl extends AbstractService<RCSBAnalysis, Long> implements RCSBService {

    @Autowired
    RCSBRepository rcsbRepository;

    @Override
    protected JpaRepository<RCSBAnalysis, Long> getDao() {
        return rcsbRepository;
    }

    @Override
    public RCSBAnalysis add(RCSBAnalysis entity) {
        return super.add(entity);
    }

    @Override
    public RCSBAnalysis update(Long id, RCSBAnalysis newRCSBAnalysis) {
        RCSBAnalysis oldRCSBAnalysis = getOne(id);
        newRCSBAnalysis.setId(id);

        return getDao().saveAndFlush(newRCSBAnalysis);
    }

    @Override
    public List<RCSBAnalysis> getRCSBAnalysisByUser(User user) {
        return rcsbRepository.findByUser(user);
    }


}

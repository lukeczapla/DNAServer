package edu.dnatools.service;

import edu.dnatools.model.User;
import edu.dnatools.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by luke on 9/9/16.
 */
@Service
@Transactional
public class UserServiceImpl extends AbstractService<User, Long> implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    protected JpaRepository<User, Long> getDao() { return userRepository; }

    @Override
    public User add(User entity) {
        return super.add(entity);
    }

    @Override
    public User update(Long id, User newUser) {
        User oldUser = getOne(id);
        newUser.setId(id);
        return getDao().saveAndFlush(newUser);
    }

}

package edu.dnatools.service;

import edu.dnatools.model.UserDetails;
import edu.dnatools.model.Role;
import edu.dnatools.model.User;
import edu.dnatools.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by luke on 6/21/16.
 */
@Service
@Transactional
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository repository;

    @Override
    public User registerNewAccount(User newUser) throws Exception {
        if (repository.findByEmail(newUser.getEmail()) != null) {
            throw new Exception("Account already exists " + newUser.getEmail());
        }
        newUser.setRole(Role.ROLE_USER);
        repository.save(newUser);
        return newUser;
    }

    @Override
    public org.springframework.security.core.userdetails.UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = repository.findByEmail(username);
        if (user == null) {
            return null;
//            throw new UsernameNotFoundException("No user found with username: " + username);
        }

        UserDetails principal = UserDetails.getBuilder()
                .firstName(user.getFirstName()).lastName(user.getLastName()).id(user.getId())
                .password(user.getPassword()).role(user.getRole()).username(user.getEmail())
                .build();

        return principal;
    }

}

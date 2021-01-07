package edu.dnatools.service;

import edu.dnatools.model.User;

/**
 * Created by luke on 6/22/16.
 */
public interface UserDetailsService extends org.springframework.security.core.userdetails.UserDetailsService {

    User registerNewAccount(User newUser) throws Exception;

}

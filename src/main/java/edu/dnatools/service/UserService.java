package edu.dnatools.service;

import edu.dnatools.model.User;

/**
 * Created by luke on 9/9/16.
 */
public interface UserService extends JpaService<User, Long> {
	User getByEmail(String email);
}

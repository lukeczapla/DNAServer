package edu.dnatools.controller;

import com.fasterxml.jackson.annotation.JsonView;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import edu.dnatools.conf.GoogleProperties;
import edu.dnatools.model.JsonViews;
import edu.dnatools.model.User;
import edu.dnatools.service.UserDetailsService;
import edu.dnatools.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by luke on 6/5/16.
 */
@RestController
@Api("User login and registration")
public class UserController {

    private static Logger log = LoggerFactory.getLogger(UserController.class);
    private static final JacksonFactory jacksonFactory = new JacksonFactory();

    @Autowired
    UserDetailsService userDetailsService;

    @Autowired
    UserService userService;
    
    @Autowired
    GoogleProperties googleProperties;



    @ApiOperation("Retrieve list of all registered users")
    @RequestMapping(value = "/conf/userlist", method = RequestMethod.GET)
    public List<User> getAll() {
        return userService.getAll();
    }


    @ApiOperation("Check user status")
    @RequestMapping(value = "/conf/user", method = RequestMethod.GET)
    public Principal user(Principal user) {
        return user;
    }



    @ApiOperation(value = "Authenticate the provided user", notes = "User information obtained from Google")
    @RequestMapping(value = "/conf/user", method = RequestMethod.POST)
    @JsonView(JsonViews.User.class)
    public ResponseEntity login(@RequestBody User user) throws Exception {
        if (user == null || user.getEmail() == null) {
            return new ResponseEntity("Invalid data", HttpStatus.BAD_REQUEST);
        }
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), jacksonFactory)
                .setAudience(Collections.singletonList(googleProperties.getClientId()))
                .build();
        log.info("Login Controller: " + user.getEmail() + " " + user.getTokenId().substring(0,5));
        GoogleIdToken idToken = verifier.verify(user.getTokenId());

        if (idToken == null) {
            return new ResponseEntity("Invalid token data", HttpStatus.BAD_REQUEST);
        } else {
            Payload payload = idToken.getPayload();
            if (payload == null || !payload.getEmail().equals(user.getEmail())) {
                return new ResponseEntity("Invalid email address", HttpStatus.BAD_REQUEST);
            }
            log.info("Verified account");
        }

        if (userDetailsService.loadUserByUsername(user.getEmail()) != null) {

            UsernamePasswordAuthenticationToken authrequest = new UsernamePasswordAuthenticationToken(user.getEmail(), null,
                    userDetailsService.loadUserByUsername(user.getEmail()).getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authrequest);

            return new ResponseEntity("Finished, authenticated", HttpStatus.OK);

        } else {

            try {
                userDetailsService.registerNewAccount(user);
            } catch (Exception e) {
                e.printStackTrace();
                return new ResponseEntity("Invalid email address", HttpStatus.BAD_REQUEST);
            }

            Set<GrantedAuthority> grant = new HashSet<GrantedAuthority>();
            grant.add(new SimpleGrantedAuthority(user.getRole().toString()));
            UsernamePasswordAuthenticationToken authrequest = new UsernamePasswordAuthenticationToken(user.getEmail(), null, grant);
            SecurityContextHolder.getContext().setAuthentication(authrequest);
            return new ResponseEntity("Created, authenticated", HttpStatus.OK);

        }
        //return new ResponseEntity("Invalid data", HttpStatus.BAD_REQUEST);
    }


    @ApiOperation("Log out current user session")
    @RequestMapping(value = "/conf/user/logout", method = RequestMethod.GET)
    public void logout(HttpSession session) {
        session.invalidate();
    }


    @RequestMapping(value = "/conf/user/test", method = RequestMethod.GET)
    public ResponseEntity test(Authentication authentication) {
        if (SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            log.info(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString());
        }

        return new ResponseEntity("Ok", HttpStatus.OK);
    }


}
